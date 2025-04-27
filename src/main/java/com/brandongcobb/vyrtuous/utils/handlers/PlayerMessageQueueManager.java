/*  PlayerMesageQueueManager.java The primary purpose of this class is to integrate
 *  Copyright (C) 2024  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.utils.handlers;

import java.util.concurrent.*;

public class PlayerMessageQueueManager {

    private final ConcurrentHashMap<Long, BlockingQueue<String>> messageQueues = new ConcurrentHashMap<>();

    public void addPlayer(Long playerLong) {
        messageQueues.put(playerLong, new LinkedBlockingQueue<>());
    }

    public void removePlayer(Long playerLong) {
        messageQueues.remove(playerLong);
    }

    public void enqueueMessage(Long playerLong, String message) {
        BlockingQueue<String> queue = messageQueues.get(playerLong);
        if (queue != null) {
            queue.offer(message);
        }
    }

    public String dequeueMessage(Long playerLong, long timeout, TimeUnit unit) throws InterruptedException {
        BlockingQueue<String> queue = messageQueues.get(playerLong);
        if (queue != null) {
            return queue.poll(timeout, unit);
        }
        return null;
    }
}
