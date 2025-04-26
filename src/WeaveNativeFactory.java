//provides a way to get the correct native interface implementation based on the os
public abstract class WeaveNativeFactory {
    //helper method to initialize the implementation with the correct library name
    private static WeaveNativeImpl initWeavePlatform(String platformLib) {
        //gets the singleton instance of the implementation
        //sets the platform library name (e.g., .dll or .so)
        WeaveNativeImpl wn = WeaveNativeImpl.WeaveNativeImpl();
        if (wn.platformLib != null) {
            wn.platformLib = platformLib;
        }

        return wn;
    }

    //the main factory method to get the native interface
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
