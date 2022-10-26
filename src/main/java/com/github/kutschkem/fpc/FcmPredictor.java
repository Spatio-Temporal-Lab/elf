/*******************************************************************************
 * Copyright (c) 2013 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
package com.github.kutschkem.fpc;

public class FcmPredictor {

    private long[] table;
    private int fcm_hash;

    public FcmPredictor(int logOfTableSize) {
        table = new long[1 << logOfTableSize];
    }

    public long getPrediction() {
        return table[fcm_hash];
    }

    public void update(long true_value) {
        table[fcm_hash] = true_value;
        fcm_hash = (int) (((fcm_hash << 6) ^ (true_value >> 48)) & (table.length - 1));
    }

}
