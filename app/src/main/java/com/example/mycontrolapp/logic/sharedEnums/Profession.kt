package com.example.mycontrolapp.logic.sharedEnums

import androidx.annotation.StringRes
import com.example.mycontrolapp.R

enum class Profession(@StringRes val labelRes: Int) {
    Yanir(R.string.prof_yanir),
    Kitchen(R.string.prof_kitchen),
    Solider(R.string.prof_soldier),
    Officer(R.string.prof_officer),
    Negev(R.string.prof_negev),
    Mag(R.string.prof_mag),
    Medic(R.string.prof_medic),
    Shooter(R.string.prof_shooter),
    GunGrenadeLauncher(R.string.prof_grenade_launcher),
    Driver(R.string.prof_driver),
    Unknown(R.string.prof_unknown);
}