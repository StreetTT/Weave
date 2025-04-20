
public abstract class WeaveNativeFactory {
    public static WeaveNative get() {
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows") || osName.contains("inux")) {
            return WeaveNativeImpl.WeaveNativeImpl();
        } else {
            return new WeaveNativeStub();
        }
    }
}
