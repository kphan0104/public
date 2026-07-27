package fr.itgp.testsproducer.adapter.out.system;

import fr.itgp.testsproducer.application.port.out.HostnameProviderPort;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class LocalHostnameAdapter implements HostnameProviderPort {

    @Override
    public String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return "localhost";
        }
    }
}
