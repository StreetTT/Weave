
public abstract class WeaveNativeFactory {
    public static WeaveNative get() {
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            return WeaveNativeImpl.WeaveNativeImpl();
        } else {
            return new WeaveNativeStub();
        }
    }
}
