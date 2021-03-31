package analytics;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class Crypto {
    public abstract InputStream decrypt(InputStream is);

    public abstract OutputStream encrypt(OutputStream os);

    public static Crypto none() {
        return new Crypto() {
            @Override
            public InputStream decrypt(InputStream is) {
                return is;
            }

            @Override
            public OutputStream encrypt(OutputStream os) {
                return os;
            }
        };
    }
}
