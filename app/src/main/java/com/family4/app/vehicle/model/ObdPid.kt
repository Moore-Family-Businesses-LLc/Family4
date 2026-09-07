package com.family4.app.vehicle.model

/**
 * Standard OBD-II Mode 01 PIDs — service 0x01 "show current data".
 *
 * Each entry carries:
 *  - [mode]   hex mode byte (usually 01)
 *  - [pid]    hex PID byte
 *  - [name]   human-readable label
 *  - [unit]   display unit string
 *  - [bytes]  how many data bytes are in the response
 *  - [decode] lambda: raw byte array → physical value (Double)
 */
enum class ObdPid(
    val mode: String,
    val pid: String,
    val label: String,
    val unit: String,
    val bytes: Int,
    val decode: (ByteArray) -> Double
) {
    RPM("01","0C","Engine RPM","rpm",2,
        { b -> ((b[0].toInt() and 0xFF) * 256.0 + (b[1].toInt() and 0xFF)) / 4.0 }),

    SPEED("01","0D","Vehicle Speed","km/h",1,
        { b -> (b[0].toInt() and 0xFF).toDouble() }),

    COOLANT_TEMP("01","05","Coolant Temp","°C",1,
        { b -> ((b[0].toInt() and 0xFF) - 40).toDouble() }),

    ENGINE_LOAD("01","04","Engine Load","%",1,
        { b -> (b[0].toInt() and 0xFF) * 100.0 / 255.0 }),

    FUEL_LEVEL("01","2F","Fuel Level","%",1,
        { b -> (b[0].toInt() and 0xFF) * 100.0 / 255.0 }),

    THROTTLE("01","11","Throttle Position","%",1,
        { b -> (b[0].toInt() and 0xFF) * 100.0 / 255.0 }),

    INTAKE_TEMP("01","0F","Intake Air Temp","°C",1,
        { b -> ((b[0].toInt() and 0xFF) - 40).toDouble() }),

    INTAKE_PRESSURE("01","0B","Intake Manifold Pressure","kPa",1,
        { b -> (b[0].toInt() and 0xFF).toDouble() }),

    MAF("01","10","Mass Air Flow","g/s",2,
        { b -> ((b[0].toInt() and 0xFF) * 256.0 + (b[1].toInt() and 0xFF)) / 100.0 }),

    OIL_TEMP("01","5C","Engine Oil Temp","°C",1,
        { b -> ((b[0].toInt() and 0xFF) - 40).toDouble() }),

    BATTERY_VOLTAGE("01","42","Battery Voltage","V",2,
        { b -> ((b[0].toInt() and 0xFF) * 256.0 + (b[1].toInt() and 0xFF)) / 1000.0 }),

    ENGINE_RUNTIME("01","1F","Engine Run Time","s",2,
        { b -> ((b[0].toInt() and 0xFF) * 256.0 + (b[1].toInt() and 0xFF)) }),

    BARO_PRESSURE("01","33","Barometric Pressure","kPa",1,
        { b -> (b[0].toInt() and 0xFF).toDouble() }),

    TORQUE_PCT("01","62","Actual Engine Torque","%",1,
        { b -> ((b[0].toInt() and 0xFF) - 125).toDouble() }),

    FUEL_PRESSURE("01","0A","Fuel Pressure","kPa",1,
        { b -> (b[0].toInt() and 0xFF) * 3.0 }),

    SHORT_FUEL_TRIM("01","06","Short Fuel Trim B1","%",1,
        { b -> ((b[0].toInt() and 0xFF) - 128) * 100.0 / 128.0 }),

    LONG_FUEL_TRIM("01","07","Long Fuel Trim B1","%",1,
        { b -> ((b[0].toInt() and 0xFF) - 128) * 100.0 / 128.0 });

    /** Formats the AT command string to send to the ELM327. */
    fun command(): String = "$mode $pid\r"
}
