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

package com.github.charithe.flake4j;

import com.github.charithe.flake4j.node.MacAddressNodeIdentifier;
import com.github.charithe.flake4j.node.NodeIdentifier;

import java.math.BigInteger;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the Flake ID generation algorithm by Boundary (https://github.com/boundary/flake)
 */
public class Flake4J {
    private static final int MAX_SEQ = 0xFFFF;
    private static final int ID_SIZE_BYTES = 16;
    private static final int NODE_ID_BYTES = 6;
    private final Lock lock = new ReentrantLock(true);
    private final byte[] nodeId;
    private final Clock clock;
    private volatile long currentTime;
    private volatile long lastTime;
    private volatile int sequence;

    /**
     * Create a new instance that attempts to use the MAC address of the system as the node identifier
     *
     * @return Flake4J instance
     * @throws SocketException       If MAC address detection fails
     * @throws IllegalStateException If a viable (non-loopback) MAC address cannot be determined
     */
    public static Flake4J newInstance() throws SocketException {
        return newInstance(MacAddressNodeIdentifier.newInstance());
    }

    /**
     * Create a new instance using the specified node identifer
     *
     * @param nodeIdentifier {@link NodeIdentifier} implementation
     * @return Flake4J instance
     */
    public static Flake4J newInstance(NodeIdentifier nodeIdentifier) {
        return newInstance(nodeIdentifier, Clock.systemUTC());
    }

    /**
     * Create a new instance using the specified node identifier and clock
     *
     * @param nodeIdentifier {@link NodeIdentifier} implementation
     * @param clock          {@link Clock} to use for time keeping
     * @return Flake4J instance
     */
    public static Flake4J newInstance(NodeIdentifier nodeIdentifier, Clock clock) {
        long tempNodeId = nodeIdentifier.get();
        byte[] nodeId = new byte[NODE_ID_BYTES];
        for (int i = 0; i < NODE_ID_BYTES; i++) {
            nodeId[i] = (byte) ((tempNodeId >> ((5 - i) * 8)) & 0xFFL);
        }
        return new Flake4J(nodeId, clock);
    }

    private Flake4J(byte[] nodeId, Clock clock) {
        this.nodeId = nodeId;
        this.clock = clock;
        this.lastTime = clock.millis();
        this.sequence = -1;
    }

    /**
     * Generate a 128-bit Flake ID
     *
     * @return byte array containing the generated ID
     */
    public byte[] generateId() {
        lock.lock();
        try {
            updateState();
            ByteBuffer idBuffer = ByteBuffer.allocate(ID_SIZE_BYTES);
            return idBuffer.putLong(currentTime).put(nodeId).putShort((short) sequence).array();
        } finally {
            lock.unlock();
        }
    }

    private void updateState() {
        currentTime = clock.millis();
        // TODO does not account for time going backwards due to NTP adjustments, user actions etc.
        if (currentTime != lastTime) {
            sequence = 0;
            lastTime = currentTime;
        } else if (sequence == MAX_SEQ) {
            throw new IllegalStateException("Sequence overflow");
        } else {
            sequence++;
        }
    }

    public static String asHexString(byte[] id) {
        StringBuilder sb = new StringBuilder();
        for (byte component : id) {
            sb.append(String.format("%02x", component));
        }
        return sb.toString();
    }

    public static String asComponentString(byte[] id) {
        ByteBuffer buffer = ByteBuffer.wrap(id);
        byte[] node = new byte[NODE_ID_BYTES];
        buffer.get(node);
        return buffer.getLong() + "-" + new BigInteger(node).longValue() + "-" + buffer.getShort();
    }
}
