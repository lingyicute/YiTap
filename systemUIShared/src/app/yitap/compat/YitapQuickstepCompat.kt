package app.yitap.compat

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import app.yitap.compatlib.ActivityManagerCompat
import app.yitap.compatlib.ActivityOptionsCompat
import app.yitap.compatlib.QuickstepCompatFactory
import app.yitap.compatlib.RemoteTransitionCompat
import app.yitap.compatlib.eleven.QuickstepCompatFactoryVR
import app.yitap.compatlib.fourteen.QuickstepCompatFactoryVU
import app.yitap.compatlib.ten.QuickstepCompatFactoryVQ
import app.yitap.compatlib.thirteen.QuickstepCompatFactoryVT
import app.yitap.compatlib.twelve.QuickstepCompatFactoryVS

object YitapQuickstepCompat {

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    @JvmField
    val ATLEAST_Q: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    @JvmField
    val ATLEAST_R: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    @JvmField
    val ATLEAST_S: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    @JvmField
    val ATLEAST_T: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @JvmField
    val ATLEAST_U: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    @JvmStatic
    val factory: QuickstepCompatFactory = when {
        ATLEAST_U -> QuickstepCompatFactoryVU()
        ATLEAST_T -> QuickstepCompatFactoryVT()
        ATLEAST_S -> QuickstepCompatFactoryVS()
        ATLEAST_R -> QuickstepCompatFactoryVR()
        ATLEAST_Q -> QuickstepCompatFactoryVQ()
        else -> error("Unsupported SDK version")
    }

    @JvmStatic
    val activityManagerCompat: ActivityManagerCompat = factory.activityManagerCompat

    @JvmStatic
    val activityOptionsCompat: ActivityOptionsCompat = factory.activityOptionsCompat

    @JvmStatic
    val remoteTransitionCompat: RemoteTransitionCompat = factory.remoteTransitionCompat
}
