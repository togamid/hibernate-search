/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.spi;

import java.util.function.Supplier;

public interface IdentifierMapping<E> {

	Object fromDocumentIdentifier(String documentId, BridgeSessionContext sessionContext);

	Object identifier(Object providedId, Supplier<? extends E> entitySupplier);
}