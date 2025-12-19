package cn.qingweico.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.*;

/**
 * @author zqw
 * @date 2025/12/19
 */
@Slf4j
public class NetworkAddressResolver {

    @Getter
    @AllArgsConstructor
    private enum OperatingSystem {
        MAC,
        WINDOWS,
        LINUX,
        OTHER;

        public static OperatingSystem detect() {
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("mac") || osName.contains("darwin")) {
                return MAC;
            } else if (osName.contains("win")) {
                return WINDOWS;
            } else if (osName.contains("nix") || osName.contains("nux") ||
                    osName.contains("aix") || osName.contains("sunos")) {
                return LINUX;
            } else {
                return OTHER;
            }
        }
    }

    private interface AddressResolutionStrategy {
        InetAddress resolve() throws SocketException;
    }

    private static class MacAddressStrategy implements AddressResolutionStrategy {
        @Override
        public InetAddress resolve() throws SocketException {
            return getFirstLocalIpv4Address();
        }
    }

    private static class DefaultAddressStrategy implements AddressResolutionStrategy {
        @Override
        public InetAddress resolve() {
            try {
                return InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new RuntimeException("Failed to resolve local host", e);
            }
        }
    }

    private static AddressResolutionStrategy createStrategy(OperatingSystem os) {
        if (OperatingSystem.MAC == os) {
            return new MacAddressStrategy();
        }
        return new DefaultAddressStrategy();
    }

    private static InetAddress getFirstLocalIpv4Address() throws SocketException {
        List<InetAddress> inets = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isLoopback() || !ni.isUp() || ni.isPointToPoint()) {
                continue;
            }

            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address) {
                    inets.add(addr);
                }
            }
        }
        if (inets.isEmpty()) {
            throw new IllegalStateException("No local IPv4 addresses found");
        }
        // 按照接口优先级排序
        inets.sort(Comparator.comparing(NetworkAddressResolver::getInterfacePriority));
        return inets.get(0);
    }

    private static int getInterfacePriority(InetAddress address) {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            if (ni != null) {
                String name = ni.getName().toLowerCase();
                // 优先级：以太网 > WiFi > 其他
                if (name.startsWith("eth") || name.startsWith("en")) {
                    return 1;
                }
                if (name.startsWith("wlan") || name.startsWith("wl")) {
                    return 2;
                }
            }
        } catch (SocketException ignored) {
        }
        return 3;
    }

    public static InetAddress getLocalAddress() {
        try {
            OperatingSystem os = OperatingSystem.detect();
            AddressResolutionStrategy strategy = createStrategy(os);
            return strategy.resolve();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve local host", e);
        }
    }

    public static void main(String[] args) throws UnknownHostException, SocketException {
        System.out.println(NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
    }

}
