package app.yitap.compatlib.twelve;

import android.app.IApplicationThread;
import android.window.IRemoteTransition;
import android.window.RemoteTransition;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import app.yitap.compatlib.ActivityManagerCompat;
import app.yitap.compatlib.ActivityOptionsCompat;
import app.yitap.compatlib.RemoteTransitionCompat;
import app.yitap.compatlib.eleven.QuickstepCompatFactoryVR;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@RequiresApi(31)
public class QuickstepCompatFactoryVS extends QuickstepCompatFactoryVR {

    @NonNull
    @Override
    public ActivityManagerCompat getActivityManagerCompat() {
        return new ActivityManagerCompatVS();
    }

    @NonNull
    @Override
    public ActivityOptionsCompat getActivityOptionsCompat() {
        return new ActivityOptionsCompatVS();
    }

    @NonNull
    @Override
    public RemoteTransitionCompat getRemoteTransitionCompat() {
        try {
            return this::createRemoteTransition;
        } catch (Throwable t) {
            return super.getRemoteTransitionCompat();
        }
    }

    // TODO remove this as it causing glitches on first launch opening/closing app
    private RemoteTransition createRemoteTransition(
            IRemoteTransition remoteTransition, IApplicationThread appThread, String debugName) {
        try {
            Class<?> remoteTransitionClass = Class.forName("android.window.RemoteTransition");
            Constructor<?> constructor =
                    remoteTransitionClass.getConstructor(
                            IRemoteTransition.class, IApplicationThread.class);
            return (RemoteTransition) constructor.newInstance(remoteTransition, appThread);
        } catch (ClassNotFoundException
                | IllegalAccessException
                | InstantiationException
                | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException("Error creating RemoteTransitionCompat" + debugName, e);
        }
    }
}
