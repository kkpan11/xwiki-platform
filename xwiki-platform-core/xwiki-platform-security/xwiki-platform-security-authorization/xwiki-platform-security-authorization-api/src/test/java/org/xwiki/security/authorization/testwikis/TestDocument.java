/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.xwiki.security.authorization.testwikis;

import java.util.Collection;

import org.xwiki.model.reference.DocumentReference;

/**
 * Public interface of test entities representing documents.
 *
 * @version $Id$
 * @since 5.0M2
 */
public interface TestDocument extends SecureTestEntity
{
    /**
     * @return the reference of the document represented by this test document.
     */
    DocumentReference getDocumentReference();

    /**
     * @return the reference of the creator of the document represented by this test entity. May be null.
     */
    DocumentReference getCreator();

    /**
     * @return the alternate description for this test document.
     */
    String getDescription();

    /**
     * @return if required rights shall be enforced
     * @since 16.10.0RC1
     */
    boolean isEnforceRequiredRights();

    /**
     * @param requiredRight the required right to add
     * @since 16.10.0RC1
     */
    void addRequiredRight(TestRequiredRight requiredRight);

    /**
     * @return the required rights
     * @since 16.10.0RC1
     */
    Collection<TestRequiredRight> getRequiredRights();
}
