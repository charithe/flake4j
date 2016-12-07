/*
 * Copyright 2016 Charith Ellawala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.charithe.flake4j.node;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;

public class MacAddressNodeIdentifier implements NodeIdentifier {
    private static final int MAC_ADDRESS_LEN = 6;
    private final byte[] macAddress;

    private MacAddressNodeIdentifier(byte[] macAddress) {
        this.macAddress = macAddress;
    }

    public static MacAddressNodeIdentifier newInstance() throws SocketException {
        byte[] macAddr =
            findViableMacAddress().orElseThrow(() -> new IllegalStateException("No viable MAC address found"));
        return newInstance(macAddr);
    }

    public static MacAddressNodeIdentifier newInstance(String interfaceName) throws SocketException {
        byte[] macAddr = getMacAddressFromInterface(interfaceName).orElseThrow(() -> new IllegalStateException(
            "Cannot get MAC address from interface " + interfaceName));
        return newInstance(macAddr);
    }

    private static MacAddressNodeIdentifier newInstance(byte[] macAddress) {
        Objects.requireNonNull(macAddress, "MAC address should not be null");
        if (macAddress.length != MAC_ADDRESS_LEN) {
            throw new IllegalArgumentException("Invalid MAC address");
        }
        return new MacAddressNodeIdentifier(macAddress);
    }

    private static Optional<byte[]> findViableMacAddress() throws SocketException {
        Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
        while (nics.hasMoreElements()) {
            NetworkInterface currentNic = nics.nextElement();
            if (!currentNic.isLoopback() && currentNic.isUp()) {
                return Optional.of(currentNic.getHardwareAddress());
            }
        }
        return Optional.empty();
    }

    private static Optional<byte[]> getMacAddressFromInterface(String networkInterface) throws SocketException {
        NetworkInterface nic = NetworkInterface.getByName(networkInterface);
        return (nic != null) ? Optional.of(nic.getHardwareAddress()) : Optional.empty();
    }

    @Override
    public long get() {
        long macAsLong = 0;
        for (int i = 0; i < MAC_ADDRESS_LEN; i++) {
            long temp = (macAddress[i] & 0xFFL) << ((5 - i) * 8);
            macAsLong |= temp;
        }
        return macAsLong;
    }
}
