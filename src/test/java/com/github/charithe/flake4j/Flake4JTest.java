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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import com.github.charithe.flake4j.node.NodeIdentifier;
import com.googlecode.junittoolbox.MultithreadingTester;
import org.junit.Test;

import java.net.SocketException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Flake4JTest {

    @Test
    public void testSequenceIncrement() {
        NodeIdentifier nodeIdentifier = () -> 123456789L;
        Clock clock = Clock.tick(Clock.systemUTC(), Duration.ofMinutes(1));
        Flake4J f4j = Flake4J.newInstance(nodeIdentifier, clock);

        Set<String> idSet = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            idSet.add(Flake4J.asHexString(f4j.generateId()));
        }
        assertThat(idSet.size(), is(equalTo(100)));
        assertThat(idSet.stream().map(id -> id.substring(16, 28)).allMatch(s -> s.equals("0000075bcd15")), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testSequenceOverflow() {
        NodeIdentifier nodeIdentifier = () -> 123456789L;
        Clock clock = Clock.tick(Clock.systemUTC(), Duration.ofMinutes(1));
        Flake4J f4j = Flake4J.newInstance(nodeIdentifier, clock);

        for (int i = 0; i <= 0xFFFF + 0x1; i++) {
            f4j.generateId();
        }
    }

    @Test
    public void testSequenceReset() throws InterruptedException {
        NodeIdentifier nodeIdentifier = () -> 123456789L;
        Flake4J f4j = Flake4J.newInstance(nodeIdentifier);
        String seq1 = Flake4J.asHexString(Arrays.copyOfRange(f4j.generateId(), 14, 15));
        Thread.sleep(10);
        String seq2 = Flake4J.asHexString(Arrays.copyOfRange(f4j.generateId(), 14, 15));
        assertThat(seq1, is(equalTo(seq2)));
    }

    @Test
    public void testThreadSafety() throws SocketException {
        Flake4J f4j = Flake4J.newInstance();
        ConcurrentHashMap<String, Integer> idMap = new ConcurrentHashMap<>();
        new MultithreadingTester().numThreads(100).numRoundsPerThread(1000).add(() -> {
            String id = Flake4J.asHexString(f4j.generateId());
            assertThat(id + " is unique", idMap.putIfAbsent(id, 1), is(nullValue()));
        }).run();
    }
}
