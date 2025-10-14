package com.example.mycontrolapp.logic.sharedEnums

enum class Profession(val label: String) {
    Yanir("Yanir"),
    Kitchen("Kitchen"),
    Solider("Solider"),
    Officer("Officer"),
    Negev("Negev"),
    Mag("Mag"),
    Medic("Medic"),
    Shooter("Shooter"),
    GunGrenadeLauncher("Grenade Launcher"),
    Driver("Driver"),
    Unknown("Unknown");

    override fun toString(): String = label
}