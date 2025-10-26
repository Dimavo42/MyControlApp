package com.example.mycontrolapp.logic.sharedEnums
import androidx.annotation.StringRes
import com.example.mycontrolapp.R
enum class Team(@StringRes val labelRes: Int) {
    Division_1(R.string.prof_yanir),
    Division_2(R.string.prof_yanir),
    Division_3(R.string.prof_yanir),
    RETK(R.string.prof_yanir),
    CommandPost(R.string.prof_yanir),
    YANIR(R.string.prof_yanir),
    Unknown(R.string.unknown_user)
}


