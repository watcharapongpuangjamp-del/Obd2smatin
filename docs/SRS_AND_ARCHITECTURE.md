# เอกสารข้อกำหนดโปรเจกต์และสถาปัตยกรรมระบบ (Project Requirements & System Architecture)

**ชื่อโปรเจกต์:** OBD2 Smart USB Diagnostic & Real-time Dashboard Application  
**แพลตฟอร์ม:** Android (Kotlin, Jetpack Compose)  
**อินเทอร์เฟซฮาร์ดแวร์:** ELM327 USB Adapter (ผ่าน USB-OTG Serial)

---

## 1. เอกสารข้อกำหนดความต้องการซอฟต์แวร์ (Software Requirements Specification - SRS)

### 1.1 วัตถุประสงค์และขอบเขต (Purpose & Scope)
แอปพลิเคชันนี้ออกแบบมาเพื่อทำหน้าที่เป็นอุปกรณ์วัดค่าแบบเรียลไทม์ (Real-time Digital Gauge) และเครื่องสแกนวิเคราะห์ข้อผิดพลาดของรถยนต์ (Diagnostic Scan Tool) โดยเชื่อมต่อกับกล่อง ECU ของรถยนต์ผ่านหัวสแกน ELM327 แบบ USB Serial สื่อสารด้วยโปรโตคอลมาตรฐาน OBD-II ทุกประเภท

### 1.2 ข้อกำหนดทางฟังก์ชัน (Functional Requirements - FR)

* **FR-01: USB Device Management & Driver Support**
  * ระบบต้องตรวจจับการเสียบสาย USB ELM327 ผ่าน USB Host / OTG ได้อัตโนมัติ
  * รองรับชิปแปลงสัญญาณ USB-to-Serial ยอดนิยมทุกตัว: FTDI (FT232R), Prolific (PL2303), Qinheng (CH340/CH341), และ Silicon Labs (CP2102/CP2105)
  * จัดการ Request Permission สิทธิ์การเข้าถึงอุปกรณ์ USB จากระบบปฏิบัติการ Android

* **FR-02: Universal Protocol Support (การรองรับโปรโตคอล)**
  * **Auto-Detection Mode:** ค้นหาและตั้งค่าโปรโตคอลที่เหมาะสมกับรถยนต์โดยอัตโนมัติ (`AT SP 0`)
  * **Manual Selection Mode:** อนุญาตให้ผู้ใช้เลือกตั้งค่าโปรโตคอลแบบกำหนดเองได้ทั้ง 10 รูปแบบมาตรฐาน (SAE J1850 PWM/VPW, ISO 9141-2, ISO 14230-4 KWP, ISO 15765-4 CAN 11/29 bit, SAE J1939)

* **FR-03: Real-time Live Data Stream (Mode 01)**
  * อ่านค่าและประมวลผลข้อมูลสดจากเครื่องยนต์ด้วยความถี่สูง (Sampling Rate >= 10 Hz บน CAN Bus):
    * Engine RPM (ความเร็วรอบเครื่องยนต์ - PID `010C`)
    * Vehicle Speed (ความเร็วรถยนต์ - PID `010D`)
    * Engine Coolant Temperature (อุณหภูมิน้ำกูลแลนท์ - PID `0105`)
    * Calculated Engine Load (ภาระเครื่องยนต์ - PID `0104`)
    * Battery / Control Module Voltage (แรงดันไฟแบตเตอรี่ - `AT RV`)

* **FR-04: Diagnostic Trouble Codes (DTCs) Scanning (Mode 03 / Mode 07)**
  * สแกนหาคำสั่งข้อผิดพลาดที่บันทึกอยู่ใน ECU (Stored DTCs - Mode 03)
  * สแกนหาคำสั่งข้อผิดพลาดที่รอการยืนยัน (Pending DTCs - Mode 07)
  * แปลงรหัส Raw Hex สตรีมเป็นรหัสมาตรฐาน OBD2 (Powertrain: P, Chassis: C, Body: B, Network: U)

* **FR-05: Reset & Service Functions (ระบบรีเซ็ต)**
  * **Clear Engine Light (Mode 04):** ส่งคำสั่งลบโค้ด DTCs เพื่อดับไฟเตือนเช็กเครื่องยนต์ (Check Engine Light / MIL)
  * **Adapter Soft Reset:** ส่งคำสั่ง Warm Start (`AT WS`) และ Reset (`AT Z`) ไปยังหัวสแกน ELM327
  * **Re-initialization:** ล้างค่าบัฟเฟอร์ในหน่วยความจำ และทำการ Handshake กับ ECU รถยนต์ใหม่ทั้งหมด

---

### 1.3 ข้อกำหนดที่ไม่ใช่ฟังก์ชัน (Non-Functional Requirements - NFR)

* **NFR-01 (Performance):** Latency ในการส่งคำสั่งและรับ response จาก USB ไม่เกิน 50 ms (ไม่รวม response time ของ ECU)
* **NFR-02 (Stability):** ระบบต้องมี Reconnection Strategy อัตโนมัติเมื่อสาย USB ดรอปหรือหลุดระหว่างการใช้งาน
* **NFR-03 (Usability):** UI แบบ High-contrast รองรับการดูค่าขณะขับขี่ ตัวเลขขนาดใหญ่ อ่านง่าย
* **NFR-04 (Architecture):** ใช้ MVVM Pattern ร่วมกับ Kotlin Coroutines & StateFlow เพื่อป้องกัน UI Thread Blocking

---

## 2. เอกสารการออกแบบสถาปัตยกรรมระบบ (System Architecture Document)

### 2.1 Tech Stack & Dependency Matrix

| Layer | Technology / Library | Version / Detail |
| :--- | :--- | :--- |
| **Language** | Kotlin | 1.9+ |
| **UI Framework** | Jetpack Compose | Material 3 Design |
| **Architecture** | MVVM + Clean Architecture principles | ViewModel, StateFlow, LiveData |
| **USB Serial** | `com.github.mik3y:usb-serial-for-android` | v3.5.2+ |
| **Async Operations**| Kotlin Coroutines & Flow | I/O Thread Pooling |

---

### 2.2 โครงสร้างชั้นสถาปัตยกรรม (Architecture Flow)

```text
[ Hardware: Car ECU ]
        ▲
        │ OBD-II Port (CAN / K-Line / J1850)
        ▼
[ Adapter: ELM327 USB ]
        ▲
        │ USB Serial (CH340 / FTDI / PL2303)
        ▼
[ Android Physical USB Host ]
        ▲
        │ UsbSerialPort (Driver Layer)
        ▼
[ ProtocolManager / SerialRepository ]  <--- Command Queue / Hex Parser
        ▲
        │ StateFlow<OBDDataState>
        ▼
[ OBDViewModel / DTCViewModel ]          <--- Business & Calculation Logic
        ▲
        │ State Observation
        ▼
[ UI Layer: Compose Dashboard ]         <--- Live Gauges & Reset Buttons
```

---

## 3. ข้อกำหนดคำสั่งและโปรโตคอล (OBD2 Protocol & Command Specification)

### 3.1 ตารางกำหนดโปรโตคอล (Protocol Command Mapping Matrix)

| Protocol Name | Command | Baud Rate | Typical Target Vehicles |
| :--- | :--- | :--- | :--- |
| **Automatic Detection** | `AT SP 0` | 38400 / 115200 | รถยนต์ทั่วไปยุคใหม่ |
| **SAE J1850 PWM** | `AT SP 1` | 41.6 kbaud | Ford รุ่นเก่า |
| **SAE J1850 VPW** | `AT SP 2` | 10.4 kbaud | GM รุ่นเก่า |
| **ISO 9141-2** | `AT SP 3` | 10.4 kbaud | Chrysler, รถยุโรป/ญี่ปุ่น ยุค 1996-2004 |
| **ISO 14230-4 KWP (5 Baud)**| `AT SP 4` | 10.4 kbaud | รถยุโรป/เอเชีย ยุค 2000-2007 |
| **ISO 14230-4 KWP (Fast Init)**| `AT SP 5` | 10.4 kbaud | รถยุโรป/เอเชีย ยุค 2000-2007 |
| **ISO 15765-4 CAN (11bit 500k)**| `AT SP 6` | 500 kbaud | รถยนต์เบนซิน/ดีเซลส่วนใหญ่ (2008+) |
| **ISO 15765-4 CAN (29bit 500k)**| `AT SP 7` | 500 kbaud | Honda, Volvo และรถยุโรปบางรุ่น |
| **ISO 15765-4 CAN (11bit 250k)**| `AT SP 8` | 250 kbaud | รถบัส, รถบรรทุกเล็ก, รถเฉพาะกลุ่ม |
| **ISO 15765-4 CAN (29bit 250k)**| `AT SP 9` | 250 kbaud | รถบัส, รถบรรทุกเล็ก |
| **SAE J1939 CAN** | `AT SP A` | 250 kbaud | รถบรรทุกใหญ่ (Heavy Duty Vehicles) |

---

### 3.2 ตารางคำสั่งการประมวลผลและการรีเซ็ต (Modes & PIDs Table)

| Operation | Mode / AT Command | Response Format | Mathematical Formula |
| :--- | :--- | :--- | :--- |
| **Initialize Adapter** | `AT Z` / `ATE0` / `ATL0` | `OK` | - |
| **Read Voltage** | `AT RV` | `13.8V` | Direct String Parse |
| **Engine RPM** | `010C` | `41 0C AA BB` | ((A * 256) + B) / 4 (rpm) |
| **Vehicle Speed** | `010D` | `41 0D AA` | A (km/h) |
| **Coolant Temp** | `0105` | `41 05 AA` | A - 40 (°C) |
| **Engine Load** | `0104` | `41 04 AA` | (A * 100) / 255 (%) |
| **Read Stored DTCs** | `03` | `43 01 07 ...` | Bitwise Decoded DTC List |
| **Clear DTCs / MIL** | `04` | `44` | Soft Reset Check Engine Lamp |
| **Adapter Soft Reset**| `AT WS` | `ELM327 v1.5` | Re-align Connection Sequence |

---

## 4. ไฟล์คอนฟิเกอเรชันระบบ (Configuration Files)

### 4.1 `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature android:name="android.hardware.usb.host" android:required="true" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="OBD2 Smart USB"
        android:supportsRtl="true"
        android:theme="@style/Theme.OBD2SmartUSB">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <!-- Intent Filter สำหรับตรวจจับการเสียบสาย USB Auto Launch -->
            <intent-filter>
                <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
            </intent-filter>

            <meta-data
                android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
                android:value="@xml/device_filter" />
        </activity>
    </application>

</manifest>
```

---

### 4.2 `res/xml/device_filter.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- FTDI Chipset (FT232R, etc.) -->
    <usb-device vendor-id="1027" />
    
    <!-- Prolific Chipset (PL2303) -->
    <usb-device vendor-id="1659" />
    
    <!-- Qinheng / WCH Chipset (CH340, CH341) -->
    <usb-device vendor-id="6790" />
    <usb-device vendor-id="10888" />
    
    <!-- Silicon Labs Chipset (CP2102, CP2105) -->
    <usb-device vendor-id="4292" />
</resources>
```

---

## 5. แผนการทดสอบระบบและเกณฑ์การยอมรับ (Test Plan & Acceptance Criteria)

### 5.1 ตาราง Test Cases Matrix

| Test Case ID | Feature / Module | Test Procedure | Expected Result | Pass/Fail |
| :--- | :--- | :--- | :--- | :--- |
| **TC-USB-01** | USB Auto-detect | เสียบสาย USB ELM327 เข้ากับอุปกรณ์ Android | แอปแจ้งเตือนขอสิทธิ์ USB และแสดง Popup พร้อมเชื่อมต่อ | |
| **TC-PROT-01**| Auto Protocol | เลือกโหมด "AUTO" แล้วกด Connect | ส่งคำสั่ง `AT SP 0` ได้รับตอบกลับ `OK` และระบุโปรโตคอลที่พบผ่าน `AT DP` | |
| **TC-PROT-02**| Manual Protocol | เลือกโหมด "CAN 11bit 500k" แล้วกด Connect | ส่งคำสั่ง `AT SP 6` สื่อสารตรงกับ ECU โดยไม่มีข้อผิดพลาด | |
| **TC-DATA-01**| Live Data Update | ส่งคำสั่ง `010C` (RPM) และ `010D` (Speed) แบบวนลูป | ค่าบนหน้าจอ Dashboard อัปเดตเปลี่ยนแปลงตามรอบเครื่องยนต์และความเร็วจริง | |
| **TC-DTC-01** | Scan DTCs | ถอดปลั๊กเซนเซอร์บางจุดในรถทดสอบ แล้วกด "Scan DTC" | ระบบสแกนพบรหัสข้อผิดพลาด เช่น `P0100` หรือ `P0113` | |
| **TC-RST-01**  | Clear DTCs | กดปุ่ม "Clear Trouble Codes" (ขณะเปิดสวิตช์ ON เครื่องดับ) | ส่งคำสั่ง Mode `04` ได้รับตอบกลับ `44` ไฟ Check Engine หน้าปัดรถดับลง | |
| **TC-RST-02**  | Soft Reset | กดปุ่ม "Reset Adapter" | ส่งคำสั่ง `AT Z` สายและพอร์ตทำการ Re-initialize พร้อมกลับมาสแตนด์บาย | |

---

## 6. แนวทางการเตรียมสภาพแวดล้อมสำหรับการพัฒนา (Developer Setup Workflow)

1. **Hardware Preparation:**
   * หัวสแกน OBD2 ELM327 USB (หัวอินเทอร์เฟซแบบสาย USB-A)
   * สายแปลง USB OTG (USB-A Female to USB-C Male / Micro-USB)
   * กล่องจำลองสัญญาณ ECU (OBD2 Simulator) หรือ รถยนต์จริงสำหรับทดสอบ
2. **IDE & Environment:**
   * Android Studio Jellyfish / Koala ขึ้นไป
   * JDK 17
   * Android SDK Min level: 26 (Android 8.0) / Target SDK: 34 หรือล่าสุด
