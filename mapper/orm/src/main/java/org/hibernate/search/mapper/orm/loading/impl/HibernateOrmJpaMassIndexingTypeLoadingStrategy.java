/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.LockModeType;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingMappingContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.loading.EntityIdentifierScroll;
import org.hibernate.search.mapper.pojo.loading.EntityLoader;
import org.hibernate.search.mapper.pojo.loading.EntityLoadingTypeGroup.GroupingType;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingThreadContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.loading.EntityLoadingTypeGroup;

public class HibernateOrmJpaMassIndexingTypeLoadingStrategy<E, I> implements
		MassIndexingEntityLoadingStrategy<E, HibernateOrmMassIndexingOptions> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String ID_PARAMETER_NAME = "ids";

	private final HibernateOrmSessionTypeContextProvider typeContextProvider;
	private final SessionFactoryImplementor sessionFactory;
	private final EntityPersister rootEntityPersister;
	private final TypeQueryFactory<E, I> queryFactory;

	HibernateOrmJpaMassIndexingTypeLoadingStrategy(HibernateOrmMassIndexingMappingContext mappingContext,
			HibernateOrmSessionTypeContextProvider typeContextContainer,
			SessionFactoryImplementor sessionFactory,
			EntityPersister rootEntityPersister, TypeQueryFactory<E, I> queryFactory) {
		this.sessionFactory = sessionFactory;
		this.rootEntityPersister = rootEntityPersister;
		this.queryFactory = queryFactory;
		this.typeContextProvider = typeContextContainer;
	}

	@Override
	public EntityLoadingTypeGroup assignGroup() {
		return (entityName1, entityType1, entityName2, entityType2) -> {
			LoadingIndexedTypeContext<?> commonSuperType1 = typeContextProvider
					.indexedForEntityName( entityName1 );
			LoadingIndexedTypeContext<?> commonSuperType2 = typeContextProvider
					.indexedForEntityName( entityName2 );

			EntityPersister entityPersister = commonSuperType1.entityPersister();
			EntityPersister otherEntityPersister = commonSuperType2.entityPersister();
			if ( HibernateOrmUtils.isSuperTypeOf( entityPersister, otherEntityPersister ) ) {
				return GroupingType.SUPER;
			}
			if ( HibernateOrmUtils.isSuperTypeOf( otherEntityPersister, entityPersister ) ) {
				return GroupingType.INCLUDED;
			}
			return GroupingType.NONE;
		};
	}

	public HibernateOrmQueryLoader<E, I> createQueryLoader(
			Set<Class<? extends E>> targetEntityTypes) {
		Set<Class<? extends E>> includedTypesFilter;
		if ( HibernateOrmUtils.targetsAllConcreteSubTypes( sessionFactory, rootEntityPersister,
				targetEntityTypes ) ) {
			// All concrete types are included, no need to filter by type.
			includedTypesFilter = Collections.emptySet();
		}
		else {
			includedTypesFilter = new HashSet<>( targetEntityTypes.size() );
			for ( Class<? extends E> includedType : targetEntityTypes ) {
				includedTypesFilter.add( includedType );
			}
		}
		return new HibernateOrmQueryLoader<>( queryFactory, includedTypesFilter );
	}

	@Override
	public EntityIdentifierScroll createIdentifierScroll(MassIndexingThreadContext<HibernateOrmMassIndexingOptions> context, Set<Class<? extends E>> includedTypes) throws InterruptedException {
		HibernateOrmQueryLoader<E, I> typeQueryLoader = createQueryLoader( includedTypes );
		return new HibernateOrmEntityIdentifierScroll( typeQueryLoader, context );
	}

	@Override
	public EntityLoader<E> createLoader(MassIndexingThreadContext<HibernateOrmMassIndexingOptions> context, Set<Class<? extends E>> includedTypes) throws InterruptedException {
		HibernateOrmQueryLoader<E, I> typeQueryLoader = createQueryLoader( includedTypes );
		return new HibernateOrmEntityLoader( typeQueryLoader, context );
	}

	private class HibernateOrmEntityIdentifierScroll<E> implements EntityIdentifierScroll {
		private final HibernateOrmQueryLoader<? super E, I> typeQueryLoader;
		private final MassIndexingThreadContext<HibernateOrmMassIndexingOptions> context;
		private Long totalCount;
		private final ScrollableResults results;
		private final int fetchSize;
		private final int batchSize;

		public HibernateOrmEntityIdentifierScroll(HibernateOrmQueryLoader<? super E, I> typeGroupLoader,
				MassIndexingThreadContext<HibernateOrmMassIndexingOptions> context) {
			this.typeQueryLoader = typeGroupLoader;
			this.context = context;

			HibernateOrmMassIndexingOptions options = context.options();
			fetchSize = options.fetchSize();
			batchSize = options.batchSize();
			long objectsLimit = options.objectsLimit();

			SharedSessionContractImplementor session = context.context( SharedSessionContractImplementor.class );
			totalCount = typeQueryLoader
					.createCountQuery( session )
					.setCacheable( false ).uniqueResult();

			if ( objectsLimit != 0 && objectsLimit < totalCount ) {
				totalCount = objectsLimit;
			}
			if ( log.isDebugEnabled() ) {
				log.debugf( "going to fetch %d primary keys", totalCount );
			}

			results = typeQueryLoader.createIdentifiersQuery( session )
					.setCacheable( false )
					.setFetchSize( fetchSize ).scroll( ScrollMode.FORWARD_ONLY );

		}

		@Override
		public long totalCount() {
			return totalCount;
		}

		@Override
		public List<?> next() {
			ArrayList<I> destinationList = new ArrayList<>( batchSize );
			long counter = 0;
			while ( results.next() ) {
				@SuppressWarnings("unchecked")
				I id = (I) results.get( 0 );
				destinationList.add( id );
				if ( destinationList.size() == batchSize ) {
					// Explicitly checking whether the TX is still open; Depending on the driver implementation new ids
					// might be produced otherwise if the driver fetches all rows up-front
					if ( !context.active() ) {
						throw log.transactionNotActiveWhileProducingIdsForBatchIndexing( context.includedEntityNames() );
					}

					return destinationList;
				}
				counter++;
				if ( counter == totalCount ) {
					break;
				}
			}
			return destinationList;
		}

		@Override
		public void close() {
			if ( results != null ) {
				results.close();
			}
		}
	}

	private class HibernateOrmEntityLoader<E> implements EntityLoader {
		private final HibernateOrmQueryLoader<? super E, I> typeQueryLoader;
		private final MassIndexingThreadContext<HibernateOrmMassIndexingOptions> context;
		private final CacheMode cacheMode;

		public HibernateOrmEntityLoader(HibernateOrmQueryLoader<? super E, I> typeGroupLoader,
				MassIndexingThreadContext<HibernateOrmMassIndexingOptions> context) {
			this.typeQueryLoader = typeGroupLoader;
			this.context = context;
			cacheMode = context.options().cacheMode();

		}

		@Override
		public List load(List identifiers) {
			SessionImplementor session = context.context( SessionImplementor.class );
			Query<? super E> query = typeQueryLoader.createLoadingQuery( session, ID_PARAMETER_NAME )
					.setParameter( ID_PARAMETER_NAME, identifiers )
					.setCacheMode( cacheMode )
					.setLockMode( LockModeType.NONE )
					.setCacheable( false )
					.setHibernateFlushMode( FlushMode.MANUAL )
					.setFetchSize( identifiers.size() );
			return query.getResultList();
		}
	}

}