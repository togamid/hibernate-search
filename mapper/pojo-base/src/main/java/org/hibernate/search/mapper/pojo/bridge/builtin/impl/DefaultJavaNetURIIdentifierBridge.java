/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.net.URI;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public final class DefaultJavaNetURIIdentifierBridge implements IdentifierBridge<URI> {

	@Override
	public String toDocumentIdentifier(URI propertyValue,
			IdentifierBridgeToDocumentIdentifierContext context) {
		return DefaultJavaNetURIValueBridge.to( propertyValue );
	}

	@Override
	public URI fromDocumentIdentifier(String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context) {
		return DefaultJavaNetURIValueBridge.from( documentIdentifier );
	}

	@Override
	public boolean isCompatibleWith(IdentifierBridge<?> other) {
		return getClass().equals( other.getClass() );
	}

}