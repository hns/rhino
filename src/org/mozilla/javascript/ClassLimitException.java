/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

 package org.mozilla.javascript;

/**
 * Thrown when creation of a compiled script fails due to Java class file
 * restrictions. Errors of this type can be handled by falling back to
 * interpreter mode.
 */
public class ClassLimitException extends RuntimeException {

    private static final long serialVersionUID = 4575316855182281347L;

    public ClassLimitException(String message) {
        super(message);
    }
}
