package com.example.mycontrolapp.logic.sharedEnums
import androidx.annotation.StringRes
import com.example.mycontrolapp.R
enum class Team(@StringRes val labelRes: Int) {
    Division_1(R.string.team_Division_1),
    Division_2(R.string.team_Division_2),
    Division_3(R.string.team_Division_3),
    RETK(R.string.team_RETK),
    CommandPost(R.string.team_CommandPost),
    YANIR(R.string.team_YANIR),
    Unknown(R.string.team_Unknown)
}


