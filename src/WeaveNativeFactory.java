
public abstract class WeaveNativeFactory {

    private static WeaveNativeImpl initWeavePlatform(String platformLib) {
        WeaveNativeImpl wn = WeaveNativeImpl.WeaveNativeImpl();
        if (wn.platformLib != null) {
            wn.platformLib = platformLib;
        }

        return wn;
    }

    public static WeaveNative get() {
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            return WeaveNativeFactory.initWeavePlatform("weave_native.dll");
        } else if(osName.contains("inux")) {
            return WeaveNativeFactory.initWeavePlatform("weave_native.so");
        } else {
            return new WeaveNativeStub();
        }
    }
}
