package io.github.karlatemp.jap.launcher;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Map;

class JPLUrlStreamHandler extends URLStreamHandler {
    private final Map<String, byte[]> data;

    JPLUrlStreamHandler(Map<String, byte[]> data) {
        this.data = data;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        if (u.getFile().equals("/$all-files")) {
            return new URLConnection(u) {
                @Override
                public Object getContent() throws IOException {
                    return new ArrayList<>(data.keySet());
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(new byte[0]);
                }

                @Override
                public void connect() throws IOException {
                }
            };
        }
        byte[] data = this.data.get(u.getFile());
        if (data != null) {
            return new URLConnection(u) {
                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(data);
                }

                @Override
                public void connect() throws IOException {
                    connected = true;
                }
            };
        }
        throw new FileNotFoundException(u.getFile());
    }
}
