/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingDocumentProducerInterceptor;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingIdentifierProducerInterceptor;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingMappingContext;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingSessionContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingSessionContext;

public final class HibernateOrmMassIndexingContext implements MassIndexingContext<HibernateOrmMassIndexingOptions> {
	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final HibernateOrmSessionTypeContextProvider typeContextProvider;
	private final List<LoadingInterceptor> identifierProducerInterceptors = new ArrayList<>();
	private final List<LoadingInterceptor> documentProducerInterceptors = new ArrayList<>();

	public HibernateOrmMassIndexingContext(HibernateOrmMassIndexingMappingContext mappingContext,
			HibernateOrmSessionTypeContextProvider typeContextContainer) {
		this.mappingContext = mappingContext;
		this.typeContextProvider = typeContextContainer;
		identifierProducerInterceptors.add( new HibernateOrmMassIndexingIdentifierProducerInterceptor( mappingContext ) );
		documentProducerInterceptors.add( new HibernateOrmMassIndexingDocumentProducerInterceptor( mappingContext ) );
	}

	@Override
	public Object indexingKey(PojoRawTypeIdentifier<?> type) {
		return type;
	}

	@Override
	public <T> HibernateOrmJpaMassIndexingTypeLoadingStrategy<? super T, ?> createIndexLoadingStrategy(
			PojoRawTypeIdentifier<? extends T> expectedType) {
		SessionFactoryImplementor sessionFactory = mappingContext.sessionFactory();

		LoadingIndexedTypeContext<? extends T> typeContext = typeContextProvider.indexedForExactType( expectedType );
		EntityPersister entityPersister = typeContext.entityPersister();

		EntityPersister rootEntityPersister = HibernateOrmUtils.toRootEntityType( sessionFactory, entityPersister );
		TypeQueryFactory<?, ?> queryFactory = TypeQueryFactory.create( sessionFactory, rootEntityPersister,
				entityPersister.getIdentifierPropertyName() );
		return new HibernateOrmJpaMassIndexingTypeLoadingStrategy( mappingContext,
				typeContextProvider, sessionFactory,
				rootEntityPersister, queryFactory );
	}

	@Override
	public String entityName(PojoRawTypeIdentifier<?> entityType) {
		LoadingIndexedTypeContext<?> typeContext = typeContextProvider.indexedForExactType( entityType );
		return typeContext.jpaEntityName();
	}

	@Override
	public Object entityIdentifier(MassIndexingSessionContext sessionContext,
			PojoRawTypeIdentifier<?> commonSuperType, Object entity) {
		Session session = ((HibernateOrmMassIndexingSessionContext) sessionContext).session();
		return session.getIdentifier( entity );
	}

	@Override
	public Object extractReferenceOrSuppress(MassIndexingSessionContext sessionContext,
			PojoRawTypeIdentifier<?> commonSuperType, Object entity, Throwable throwable) {
		String entityName = entityName( commonSuperType );
		Object identifier = entityIdentifier( sessionContext, commonSuperType, entity );
		return EntityReferenceFactory.safeCreateEntityReference( mappingContext.entityReferenceFactory(),
				entityName, identifier, throwable::addSuppressed );
	}

	@Override
	public List identifierInterceptors() {
		return identifierProducerInterceptors;
	}

	@Override
	public List documentInterceptors() {
		return documentProducerInterceptors;
	}

}