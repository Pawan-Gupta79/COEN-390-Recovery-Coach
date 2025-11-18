#include <ArduinoBLE.h>
#include <Wire.h>
#include "MAX30105.h"
#include "heartRate.h"               // checkForBeat()
#include <Arduino_HS300x.h>          // HS300x temp/humidity
#include <Arduino_BMI270_BMM150.h>   // BMI270 IMU
#include <math.h>

// -----------------------------------------------------------------------------
// BLE SERVICE & CHARACTERISTICS
// -----------------------------------------------------------------------------

// Custom service (must match Android)
BLEService hrService("12345678-1234-5678-1234-56789abcdef0");

// Heart rate: 1-byte BPM (0–255), notify + read
BLEUnsignedCharCharacteristic hrChar(
  "12345678-1234-5678-1234-56789abcdef1",
  BLERead | BLENotify
);

// Temperature: SINT16, value = °C * 100 (e.g. 23.45°C → 2345)
BLEShortCharacteristic tempChar(
  "12345678-1234-5678-1234-56789abcdef2",
  BLERead | BLENotify
);

// Humidity: SINT16, value = %RH * 100 (e.g. 45.67% → 4567)
BLEShortCharacteristic humChar(
  "12345678-1234-5678-1234-56789abcdef3",
  BLERead | BLENotify
);

// Control: command byte from phone
//   0x01 = measure temp/hum once
//   0x10 = start IMU/pedometer
//   0x11 = stop IMU/pedometer
BLEByteCharacteristic controlChar(
  "12345678-1234-5678-1234-56789abcdef4",
  BLEWrite
);

// Steps: UINT32 step count, notify + read
BLEUnsignedLongCharacteristic stepsChar(
  "12345678-1234-5678-1234-56789abcdef5",
  BLERead | BLENotify
);

// Command values (must match Android app)
const uint8_t CMD_MEASURE_ENV = 0x01;
const uint8_t CMD_START_IMU   = 0x10;
const uint8_t CMD_STOP_IMU    = 0x11;

// -----------------------------------------------------------------------------
// MAX3010x HEART RATE SECTION
// -----------------------------------------------------------------------------

MAX30105 particleSensor;

// IBI / BPM buffers
const byte IBI_SIZE = 10;
uint16_t ibis[IBI_SIZE];
byte ibiIndex = 0;
byte ibiCount = 0;

// Timing
uint32_t lastBeat     = 0;
uint32_t lastGoodBeat = 0;
uint32_t lastDisplay  = 0;

// BPM tracking
float bpmFiltered = 0.0f;
bool  bpmInit     = false;

// Tunable parameters
const float    MIN_BPM           = 45.0f;
const float    MAX_BPM           = 170.0f;
const float    EMA_ALPHA         = 0.3f;
const float    MAX_STEP_PER_BEAT = 12.0f;

// IBI sanity range based on BPM range
const uint16_t IBI_MIN_MS = (uint16_t)(60000.0f / MAX_BPM);
const uint16_t IBI_MAX_MS = (uint16_t)(60000.0f / MIN_BPM);

// Output update rate (Serial + BLE)
const uint32_t DISPLAY_EVERY_MS = 500;

int displayedBPM = 0;

// Helper: median of IBI buffer
float medianIBI()
{
  if (ibiCount == 0) return 0.0f;

  uint16_t temp[IBI_SIZE];
  for (byte i = 0; i < ibiCount; i++)
    temp[i] = ibis[i];

  // Insertion sort
  for (byte i = 1; i < ibiCount; i++)
  {
    uint16_t key = temp[i];
    byte j = i;
    while (j > 0 && temp[j - 1] > key)
    {
      temp[j] = temp[j - 1];
      j--;
    }
    temp[j] = key;
  }

  if (ibiCount % 2 == 1)
  {
    return (float)temp[ibiCount / 2];
  }
  else
  {
    uint16_t a = temp[(ibiCount / 2) - 1];
    uint16_t b = temp[ibiCount / 2];
    return (float)((a + b) / 2.0f);
  }
}

// -----------------------------------------------------------------------------
// HS300x TEMPERATURE / HUMIDITY SECTION
// -----------------------------------------------------------------------------

bool envSensorAvailable = false;  // true if HS300x.begin() succeeds

// -----------------------------------------------------------------------------
// BMI270 PEDOMETER SECTION
// -----------------------------------------------------------------------------

bool imuAvailable = false;   // true if IMU.begin() succeeds
bool imuEnabled   = false;   // true while IMU/pedometer is "on" (between start & stop)

// Step count
uint32_t stepCount = 0;

// Low-pass filter coeff (0..1)
const float alpha = 0.90f;

// Threshold in "g above gravity" to count a step
const float stepThreshold = 0.35f;

// Min time between steps in ms
const uint32_t minStepIntervalMs = 250;

// Internal state
float    filteredAcc     = 0.0f;
bool     stepHigh        = false;
uint32_t lastStepTime    = 0;

// -----------------------------------------------------------------------------
// SETUP
// -----------------------------------------------------------------------------

void setup()
{
  Serial.begin(115200);
  //while (!Serial) {
  //  ; // wait for Serial on some boards
  //}

  Serial.println("Nano 33 BLE Sense Rev2 - HR + Env + Pedometer");

  // ---- MAX3010x Heart Rate sensor ----
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST))
  {
    Serial.println("MAX3010x not found. Check wiring.");
    while (1);
  }

  particleSensor.setup();
  particleSensor.setPulseAmplitudeRed(0x1F);
  particleSensor.setPulseAmplitudeGreen(0);

  uint32_t now = millis();
  lastBeat     = now;
  lastGoodBeat = 0;
  lastDisplay  = now;

  // ---- HS300x Env sensor ----
  if (!HS300x.begin()) {
    Serial.println("HS300x not detected (temp/humidity will be unavailable).");
    envSensorAvailable = false;
  } else {
    Serial.println("HS300x initialized.");
    envSensorAvailable = true;
  }

  // ---- BMI270 IMU for pedometer ----
  if (!IMU.begin()) {
    Serial.println("IMU (BMI270) not detected (pedometer disabled).");
    imuAvailable = false;
  } else {
    Serial.println("IMU (BMI270) initialized.");
    imuAvailable = true;
  }

  // ---- BLE init ----
  if (!BLE.begin())
  {
    Serial.println("Starting BLE failed!");
    while (1);
  }

  BLE.setLocalName("NanoHR");
  BLE.setDeviceName("NanoHR");
  BLE.setAdvertisedService(hrService);

  // Add characteristics to service
  hrService.addCharacteristic(hrChar);
  hrService.addCharacteristic(tempChar);
  hrService.addCharacteristic(humChar);
  hrService.addCharacteristic(controlChar);
  hrService.addCharacteristic(stepsChar);

  // Add service
  BLE.addService(hrService);

  // Initialize characteristic values
  hrChar.writeValue((uint8_t)0);
  tempChar.writeValue((int16_t)0);
  humChar.writeValue((int16_t)0);
  stepsChar.writeValue((uint32_t)0);

  // Start advertising
  BLE.advertise();

  Serial.println("BLE peripheral started. Advertised as 'NanoHR'.");
}

// -----------------------------------------------------------------------------
// CONTROL: handle commands from phone via controlChar
// -----------------------------------------------------------------------------

void handleControl()
{
  if (!controlChar.written()) {
    return;   // no new command
  }

  uint8_t cmd = 0;
  controlChar.readValue(cmd);   // explicit read of written value

  switch (cmd)
  {
    case CMD_MEASURE_ENV:
      Serial.println("CMD_MEASURE_ENV received");
      if (envSensorAvailable) {
        float tC = HS300x.readTemperature();
        float hR = HS300x.readHumidity();

        int16_t t100 = (int16_t)(tC * 100.0f);
        int16_t h100 = (int16_t)(hR * 100.0f);

        tempChar.writeValue(t100);
        humChar.writeValue(h100);

        Serial.print("Measured Temp: ");
        Serial.print(tC, 2);
        Serial.print(" °C, Humidity: ");
        Serial.print(hR, 2);
        Serial.println(" %RH");
      } else {
        Serial.println("Env sensor not available.");
        tempChar.writeValue((int16_t)0);
        humChar.writeValue((int16_t)0);
      }
      break;

    case CMD_START_IMU:
      Serial.println("CMD_START_IMU received");
      if (imuAvailable) {
        imuEnabled     = true;
        stepCount      = 0;
        filteredAcc    = 0.0f;
        stepHigh       = false;
        lastStepTime   = 0;
        stepsChar.writeValue((uint32_t)0);
      } else {
        Serial.println("IMU not available.");
      }
      break;

    case CMD_STOP_IMU:
      Serial.println("CMD_STOP_IMU received");
      imuEnabled = false;
      break;

    default:
      Serial.print("Unknown CMD: 0x");
      Serial.println(cmd, HEX);
      break;
  }
}

// -----------------------------------------------------------------------------
// HEART RATE: update from MAX3010x
// -----------------------------------------------------------------------------

void updateHeartRate(uint32_t now)
{
  long irValue = particleSensor.getIR();
  bool beatDetected = checkForBeat(irValue);

  if (beatDetected)
  {
    uint32_t delta = now - lastBeat;
    lastBeat = now;

    // IBI sanity check
    if (delta >= IBI_MIN_MS && delta <= IBI_MAX_MS)
    {
      // Store IBI into ring buffer
      ibis[ibiIndex++] = (uint16_t)delta;
      if (ibiIndex >= IBI_SIZE) ibiIndex = 0;
      if (ibiCount < IBI_SIZE) ibiCount++;

      float bpmCandidate = 0.0f;

      if (ibiCount >= 3)
      {
        float medIBI = medianIBI();
        if (medIBI > 0.0f)
          bpmCandidate = 60000.0f / medIBI;
      }
      else
      {
        bpmCandidate = 60000.0f / (float)delta;
      }

      if (bpmCandidate > 0.0f &&
          bpmCandidate > MIN_BPM &&
          bpmCandidate < MAX_BPM)
      {
        if (!bpmInit)
        {
          bpmFiltered = bpmCandidate;
          bpmInit = true;
        }
        else
        {
          float upper = bpmFiltered + MAX_STEP_PER_BEAT;
          float lower = bpmFiltered - MAX_STEP_PER_BEAT;

          if (bpmCandidate > upper) bpmCandidate = upper;
          else if (bpmCandidate < lower) bpmCandidate = lower;

          bpmFiltered = bpmFiltered + EMA_ALPHA * (bpmCandidate - bpmFiltered);
        }

        lastGoodBeat = now;
      }
      // else: candidate rejected as glitch
    }
    // else: IBI out of plausible range
  }

  int bpmToShow = 0;
  if (bpmInit)
    bpmToShow = (int)(bpmFiltered + 0.5f);
  else
    bpmToShow = 0;

  displayedBPM = bpmToShow;
  if (displayedBPM < 0)   displayedBPM = 0;
  if (displayedBPM > 255) displayedBPM = 255;
}

// -----------------------------------------------------------------------------
// PEDOMETER: update from BMI270 (only when imuEnabled == true)
// -----------------------------------------------------------------------------

void updatePedometer(uint32_t now)
{
  if (!imuAvailable || !imuEnabled) return;

  float ax, ay, az;
  if (!IMU.accelerationAvailable()) return;

  IMU.readAcceleration(ax, ay, az);    // units in g

  float mag    = sqrt(ax * ax + ay * ay + az * az);  // |a|
  float accNoG = fabs(mag - 1.0f);                   // roughly remove gravity

  // Low-pass
  filteredAcc = alpha * filteredAcc + (1.0f - alpha) * accNoG;

  // Step detection (rising edge + rate limit)
  if (filteredAcc > stepThreshold) {
    if (!stepHigh && (now - lastStepTime) > minStepIntervalMs) {
      stepCount++;
      lastStepTime = now;
      stepHigh = true;

      // Update BLE characteristic (notify)
      stepsChar.writeValue((uint32_t)stepCount);
    }
  } else {
    stepHigh = false;
  }
}

// -----------------------------------------------------------------------------
// LOOP: canonical ArduinoBLE peripheral pattern
// -----------------------------------------------------------------------------

void loop()
{
  // Listen for BLE central connections
  BLEDevice central = BLE.central();

  if (central) {
    Serial.print("Connected to central: ");
    Serial.println(central.address());

    while (central.connected()) {
      BLE.poll();  // process BLE events

      uint32_t now = millis();

      // Handle any new commands from phone
      handleControl();

      // Update heart rate and pedometer
      updateHeartRate(now);
      updatePedometer(now);

      // Periodic output & HR notify
      if ((now - lastDisplay) >= DISPLAY_EVERY_MS)
      {
        lastDisplay = now;

        // Serial debug
        Serial.print("BPM: ");
        Serial.print(displayedBPM);
        Serial.print("  Steps: ");
        Serial.println(stepCount);

        // BLE: update HR characteristic
        hrChar.writeValue((uint8_t)displayedBPM);
      }

      delay(10);  // slight throttle
    }

    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
  }
}
