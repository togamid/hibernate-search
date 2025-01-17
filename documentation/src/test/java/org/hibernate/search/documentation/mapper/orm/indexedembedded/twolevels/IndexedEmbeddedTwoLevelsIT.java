/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.twolevels;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class IndexedEmbeddedTwoLevelsIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, Author.class, Address.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setId( 1 );
			book.setTitle( "Robots Of Dawn" );

			Author author = new Author();
			author.setId( 1 );
			author.setName( "Isaac Asimov" );
			book.getAuthors().add( author );
			author.getBooks().add( book );

			Address address = new Address();
			address.setCountry( "Russia" );
			address.setCity( "Petrovichi" );
			author.setPlaceOfBirth( address );

			entityManager.persist( author );
			entityManager.persist( book );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.and(
							f.match().field( "title" ).matching( "robot" ),
							f.match().field( "authors.name" ).matching( "isaac" ),
							f.match().field( "authors.placeOfBirth.country" ).matching( "russia" )
					) )
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 );
		} );
	}

}
