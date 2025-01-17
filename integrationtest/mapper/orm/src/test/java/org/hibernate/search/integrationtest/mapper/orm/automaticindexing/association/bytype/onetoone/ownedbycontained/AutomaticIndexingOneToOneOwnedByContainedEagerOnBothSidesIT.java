/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.onetoone.ownedbycontained;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingSingleValuedAssociationBaseIT;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.ContainerPrimitives;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.MultiValuedPropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.PropertyAccessor;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

/**
 * Test automatic indexing caused by single-valued association updates
 * or by updates of associated (contained) entities,
 * with a {@code @OneToOne} association owned by the contained side,
 * with eager associations on both sides.
 */
@TestForIssue(jiraKey = "HSEARCH-4305")
public class AutomaticIndexingOneToOneOwnedByContainedEagerOnBothSidesIT
		extends AbstractAutomaticIndexingSingleValuedAssociationBaseIT<
						AutomaticIndexingOneToOneOwnedByContainedEagerOnBothSidesIT.IndexedEntity,
						AutomaticIndexingOneToOneOwnedByContainedEagerOnBothSidesIT.ContainingEntity,
						AutomaticIndexingOneToOneOwnedByContainedEagerOnBothSidesIT.ContainingEmbeddable,
						AutomaticIndexingOneToOneOwnedByContainedEagerOnBothSidesIT.ContainedEntity,
						AutomaticIndexingOneToOneOwnedByContainedEagerOnBothSidesIT.ContainedEmbeddable
				> {

	public AutomaticIndexingOneToOneOwnedByContainedEagerOnBothSidesIT() {
		super( IndexedEntity.PRIMITIVES, ContainingEntity.PRIMITIVES, ContainingEmbeddable.PRIMITIVES,
				ContainedEntity.PRIMITIVES, ContainedEmbeddable.PRIMITIVES );
	}

	@Override
	protected boolean isAssociationMultiValuedOnContainedSide() {
		return false;
	}

	@Override
	protected boolean isAssociationOwnedByContainedSide() {
		return true;
	}

	@Override
	protected boolean isAssociationLazyOnContainingSide() {
		return false;
	}

	@Override
	public void setup(OrmSetupHelper.SetupContext setupContext,
			ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		super.setup( setupContext, dataClearConfig );
		// Avoid problems with deep chains of eager associations in ORM 6
		// See https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#fetch-circularity-determination
		// See https://hibernate.zulipchat.com/#narrow/stream/132094-hibernate-orm-dev/topic/lazy.20associations.20with.20ORM.206
		setupContext.withProperty( AvailableSettings.MAX_FETCH_DEPTH, 1 );

		// We're simulating a mappedBy with two associations (see comments in annotation mapping),
		// so we need to clear one side before we can delete entities.
		dataClearConfig.preClear( ContainingEntity.class, containing -> {
			containing.setContainedElementCollectionAssociationsIndexedEmbedded( null );
			containing.setContainedElementCollectionAssociationsNonIndexedEmbedded( null );
		} );
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		private String nonIndexedField;

		@OneToOne
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent")
		@IndexedEmbedded(includePaths = {
				"containedIndexedEmbedded.indexedField",
				"containedIndexedEmbedded.indexedElementCollectionField",
				"containedIndexedEmbedded.containedDerivedField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.indexedField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.indexedElementCollectionField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.containedDerivedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedElementCollectionField",
				"containedIndexedEmbeddedNoReindexOnUpdate.containedDerivedField",
				"containedIndexedEmbeddedWithCast.indexedField",
				"embeddedAssociations.containedIndexedEmbedded.indexedField",
				"embeddedAssociations.containedIndexedEmbedded.indexedElementCollectionField",
				"embeddedAssociations.containedIndexedEmbedded.containedDerivedField",
				"containedElementCollectionAssociationsIndexedEmbedded.indexedField",
				"containedElementCollectionAssociationsIndexedEmbedded.indexedElementCollectionField",
				"containedElementCollectionAssociationsIndexedEmbedded.containedDerivedField",
				"crossEntityDerivedField"
		})
		private ContainingEntity child;

		@OneToOne(mappedBy = "containingAsIndexedEmbedded")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private ContainedEntity containedIndexedEmbedded;

		@OneToOne(mappedBy = "containingAsNonIndexedEmbedded")
		private ContainedEntity containedNonIndexedEmbedded;

		@OneToOne(mappedBy = "containingAsIndexedEmbeddedShallowReindexOnUpdate")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
		private ContainedEntity containedIndexedEmbeddedShallowReindexOnUpdate;

		@OneToOne(mappedBy = "containingAsIndexedEmbeddedNoReindexOnUpdate")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private ContainedEntity containedIndexedEmbeddedNoReindexOnUpdate;

		@OneToOne(mappedBy = "containingAsUsedInCrossEntityDerivedProperty")
		private ContainedEntity containedUsedInCrossEntityDerivedProperty;

		@OneToOne(mappedBy = "containingAsIndexedEmbeddedWithCast", targetEntity = ContainedEntity.class)
		@IndexedEmbedded(includePaths = { "indexedField" }, targetType = ContainedEntity.class)
		private Object containedIndexedEmbeddedWithCast;

		@IndexedEmbedded
		@Embedded
		private ContainingEmbeddable embeddedAssociations;

		/*
		 * No mappedBy here. The inverse side of associations within an element collection cannot use mappedBy.
		 * If they do, Hibernate ORM will fail (throw an exception) while attempting to walk down the mappedBy path,
		 * because it assumes the prefix of that path is an embeddable,
		 * and in this case it is a List.
		 * TODO use mappedBy when the above gets fixed in Hibernate ORM
		 */
		@OneToOne
		@JoinColumn(name = "CECAssocIdxEmb")
		@AssociationInverseSide(inversePath = @ObjectPath({
				@PropertyValue(propertyName = "elementCollectionAssociations"),
				@PropertyValue(propertyName = "embContainingAsIndexedEmbedded")
		}))
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private ContainedEntity containedElementCollectionAssociationsIndexedEmbedded;

		/*
		 * No mappedBy here. Same reason as just above.
		 * TODO use mappedBy when the above gets fixed in Hibernate ORM
		 */
		@OneToOne
		@JoinColumn(name = "CECAssocNonIdxEmb")
		@AssociationInverseSide(inversePath = @ObjectPath({
				@PropertyValue(propertyName = "elementCollectionAssociations"),
				@PropertyValue(propertyName = "embContainingAsNonIndexedEmbedded")
		}))
		private ContainedEntity containedElementCollectionAssociationsNonIndexedEmbedded;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public ContainingEntity getParent() {
			return parent;
		}

		public void setParent(ContainingEntity parent) {
			this.parent = parent;
		}

		public ContainingEntity getChild() {
			return child;
		}

		public void setChild(ContainingEntity child) {
			this.child = child;
		}

		public ContainedEntity getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(ContainedEntity containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public ContainedEntity getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(ContainedEntity containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		public ContainedEntity getContainedIndexedEmbeddedShallowReindexOnUpdate() {
			return containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedShallowReindexOnUpdate(
				ContainedEntity containedIndexedEmbeddedShallowReindexOnUpdate) {
			this.containedIndexedEmbeddedShallowReindexOnUpdate = containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public ContainedEntity getContainedIndexedEmbeddedNoReindexOnUpdate() {
			return containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedNoReindexOnUpdate(
				ContainedEntity containedIndexedEmbeddedNoReindexOnUpdate) {
			this.containedIndexedEmbeddedNoReindexOnUpdate = containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public ContainedEntity getContainedUsedInCrossEntityDerivedProperty() {
			return containedUsedInCrossEntityDerivedProperty;
		}

		public void setContainedUsedInCrossEntityDerivedProperty(
				ContainedEntity containedUsedInCrossEntityDerivedProperty) {
			this.containedUsedInCrossEntityDerivedProperty = containedUsedInCrossEntityDerivedProperty;
		}

		public Object getContainedIndexedEmbeddedWithCast() {
			return containedIndexedEmbeddedWithCast;
		}

		public void setContainedIndexedEmbeddedWithCast(Object containedIndexedEmbeddedWithCast) {
			this.containedIndexedEmbeddedWithCast = containedIndexedEmbeddedWithCast;
		}

		public ContainingEmbeddable getEmbeddedAssociations() {
			return embeddedAssociations;
		}

		public void setEmbeddedAssociations(ContainingEmbeddable embeddedAssociations) {
			this.embeddedAssociations = embeddedAssociations;
		}

		public ContainedEntity getContainedElementCollectionAssociationsIndexedEmbedded() {
			return containedElementCollectionAssociationsIndexedEmbedded;
		}

		public void setContainedElementCollectionAssociationsIndexedEmbedded(
				ContainedEntity containedElementCollectionAssociationsIndexedEmbedded) {
			this.containedElementCollectionAssociationsIndexedEmbedded = containedElementCollectionAssociationsIndexedEmbedded;
		}

		public ContainedEntity getContainedElementCollectionAssociationsNonIndexedEmbedded() {
			return containedElementCollectionAssociationsNonIndexedEmbedded;
		}

		public void setContainedElementCollectionAssociationsNonIndexedEmbedded(
				ContainedEntity containedElementCollectionAssociationsNonIndexedEmbedded) {
			this.containedElementCollectionAssociationsNonIndexedEmbedded = containedElementCollectionAssociationsNonIndexedEmbedded;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath({
						@PropertyValue(propertyName = "containedUsedInCrossEntityDerivedProperty"),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField1")
				}),
				@ObjectPath({
						@PropertyValue(propertyName = "containedUsedInCrossEntityDerivedProperty"),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField2")
				})
		})
		public Optional<String> getCrossEntityDerivedField() {
			return containedUsedInCrossEntityDerivedProperty == null
					? Optional.empty()
					: computeDerived( Stream.of(
							containedUsedInCrossEntityDerivedProperty.getFieldUsedInCrossEntityDerivedField1(),
							containedUsedInCrossEntityDerivedProperty.getFieldUsedInCrossEntityDerivedField2()
					) );
		}

		static final ContainingEntityPrimitives<ContainingEntity, ContainingEmbeddable, ContainedEntity> PRIMITIVES = new ContainingEntityPrimitives<ContainingEntity, ContainingEmbeddable, ContainedEntity>() {
			@Override
			public Class<ContainingEntity> entityClass() {
				return ContainingEntity.class;
			}

			@Override
			public ContainingEntity newInstance(int id) {
				ContainingEntity entity = new ContainingEntity();
				entity.setId( id );
				return entity;
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainingEntity> child() {
				return PropertyAccessor.create( ContainingEntity::setChild );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainingEntity> parent() {
				return PropertyAccessor.create( ContainingEntity::setParent );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbedded,
						ContainingEntity::getContainedIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEntity::setContainedNonIndexedEmbedded,
						ContainingEntity::getContainedNonIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedShallowReindexOnUpdate() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbeddedShallowReindexOnUpdate,
						ContainingEntity::getContainedIndexedEmbeddedShallowReindexOnUpdate );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedNoReindexOnUpdate() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbeddedNoReindexOnUpdate,
						ContainingEntity::getContainedIndexedEmbeddedNoReindexOnUpdate );
			}


			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedUsedInCrossEntityDerivedProperty() {
				return PropertyAccessor.create( ContainingEntity::setContainedUsedInCrossEntityDerivedProperty,
						ContainingEntity::getContainedUsedInCrossEntityDerivedProperty );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedIndexedEmbeddedWithCast() {
				return PropertyAccessor.create( ContainingEntity::setContainedIndexedEmbeddedWithCast );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainingEmbeddable> embeddedAssociations() {
				return PropertyAccessor.create( ContainingEntity::setEmbeddedAssociations, ContainingEntity::getEmbeddedAssociations );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedElementCollectionAssociationsIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEntity::setContainedElementCollectionAssociationsIndexedEmbedded,
						ContainingEntity::getContainedElementCollectionAssociationsIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainingEntity, ContainedEntity> containedElementCollectionAssociationsNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEntity::setContainedElementCollectionAssociationsNonIndexedEmbedded,
						ContainingEntity::getContainedElementCollectionAssociationsNonIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainingEntity, String> nonIndexedField() {
				return PropertyAccessor.create( ContainingEntity::setNonIndexedField );
			}
		};
	}

	public static class ContainingEmbeddable {

		@OneToOne(mappedBy = "embeddedAssociations.embContainingAsIndexedEmbedded")
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				name = "containedIndexedEmbedded")
		// TODO Remove the "emb" prefix from this field when HHH-15604 gets fixed (it's just a workaround)
		private ContainedEntity embContainedIndexedEmbedded;

		@OneToOne(mappedBy = "embeddedAssociations.embContainingAsNonIndexedEmbedded")
		// TODO Remove the "emb" prefix from this field when HHH-15604 gets fixed (it's just a workaround)
		private ContainedEntity embContainedNonIndexedEmbedded;

		public ContainedEntity getEmbContainedIndexedEmbedded() {
			return embContainedIndexedEmbedded;
		}

		public void setEmbContainedIndexedEmbedded(ContainedEntity embContainedIndexedEmbedded) {
			this.embContainedIndexedEmbedded = embContainedIndexedEmbedded;
		}

		public ContainedEntity getEmbContainedNonIndexedEmbedded() {
			return embContainedNonIndexedEmbedded;
		}

		public void setEmbContainedNonIndexedEmbedded(ContainedEntity embContainedNonIndexedEmbedded) {
			this.embContainedNonIndexedEmbedded = embContainedNonIndexedEmbedded;
		}

		static final ContainingEmbeddablePrimitives<ContainingEmbeddable, ContainedEntity> PRIMITIVES = new ContainingEmbeddablePrimitives<ContainingEmbeddable, ContainedEntity>() {
			@Override
			public ContainingEmbeddable newInstance() {
				return new ContainingEmbeddable();
			}

			@Override
			public PropertyAccessor<ContainingEmbeddable, ContainedEntity> containedIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEmbeddable::setEmbContainedIndexedEmbedded,
						ContainingEmbeddable::getEmbContainedIndexedEmbedded
				);
			}

			@Override
			public PropertyAccessor<ContainingEmbeddable, ContainedEntity> containedNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainingEmbeddable::setEmbContainedNonIndexedEmbedded,
						ContainingEmbeddable::getEmbContainedNonIndexedEmbedded
				);
			}
		};
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ContainingEntity {

		static final String INDEX = "IndexedEntity";

		static final IndexedEntityPrimitives<IndexedEntity> PRIMITIVES = new IndexedEntityPrimitives<IndexedEntity>() {
			@Override
			public Class<IndexedEntity> entityClass() {
				return IndexedEntity.class;
			}

			@Override
			public String indexName() {
				return IndexedEntity.INDEX;
			}

			@Override
			public IndexedEntity newInstance(int id) {
				IndexedEntity entity = new IndexedEntity();
				entity.setId( id );
				return entity;
			}
		};
	}

	@Entity(name = "contained")
	public static class ContainedEntity {
		@Id
		private Integer id;

		@OneToOne
		@JoinColumn(name = "CIndexedEmbedded")
		private ContainingEntity containingAsIndexedEmbedded;

		@OneToOne
		@JoinColumn(name = "CNonIndexedEmbedded")
		private ContainingEntity containingAsNonIndexedEmbedded;

		@OneToOne
		@JoinColumn(name = "CIndexedEmbeddedSROU")
		private ContainingEntity containingAsIndexedEmbeddedShallowReindexOnUpdate;

		@OneToOne
		@JoinColumn(name = "CIndexedEmbeddedNROU")
		private ContainingEntity containingAsIndexedEmbeddedNoReindexOnUpdate;

		@OneToOne
		@JoinColumn(name = "CCrossEntityDerived")
		private ContainingEntity containingAsUsedInCrossEntityDerivedProperty;

		@OneToOne(targetEntity = ContainingEntity.class)
		@JoinColumn(name = "CIndexedEmbeddedCast")
		private Object containingAsIndexedEmbeddedWithCast;

		@Embedded
		private ContainedEmbeddable embeddedAssociations;

		@ElementCollection
		@Embedded
		@OrderColumn(name = "idx")
		@CollectionTable(name = "c_ECAssoc")
		private List<ContainedEmbeddable> elementCollectionAssociations = new ArrayList<>();

		@Basic
		@GenericField
		private String indexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "indexedECF")
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@Basic
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private String nonIndexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "nonIndexedECF")
		@Column(name = "nonIndexedECF")
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private List<String> nonIndexedElementCollectionField = new ArrayList<>();

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUIContainedDF1")
		private String fieldUsedInContainedDerivedField1;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUIContainedDF2")
		private String fieldUsedInContainedDerivedField2;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUICrossEntityDF1")
		private String fieldUsedInCrossEntityDerivedField1;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUICrossEntityDF2")
		private String fieldUsedInCrossEntityDerivedField2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public void setContainingAsIndexedEmbedded(ContainingEntity containingAsIndexedEmbedded) {
			this.containingAsIndexedEmbedded = containingAsIndexedEmbedded;
		}

		public ContainingEntity getContainingAsNonIndexedEmbedded() {
			return containingAsNonIndexedEmbedded;
		}

		public void setContainingAsNonIndexedEmbedded(ContainingEntity containingAsNonIndexedEmbedded) {
			this.containingAsNonIndexedEmbedded = containingAsNonIndexedEmbedded;
		}

		public ContainingEntity getContainingAsIndexedEmbeddedShallowReindexOnUpdate() {
			return containingAsIndexedEmbeddedShallowReindexOnUpdate;
		}

		public void setContainingAsIndexedEmbeddedShallowReindexOnUpdate(
				ContainingEntity containingAsIndexedEmbeddedShallowReindexOnUpdate) {
			this.containingAsIndexedEmbeddedShallowReindexOnUpdate = containingAsIndexedEmbeddedShallowReindexOnUpdate;
		}

		public ContainingEntity getContainingAsIndexedEmbeddedNoReindexOnUpdate() {
			return containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainingAsIndexedEmbeddedNoReindexOnUpdate(
				ContainingEntity containingAsIndexedEmbeddedNoReindexOnUpdate) {
			this.containingAsIndexedEmbeddedNoReindexOnUpdate = containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public ContainingEntity getContainingAsUsedInCrossEntityDerivedProperty() {
			return containingAsUsedInCrossEntityDerivedProperty;
		}

		public void setContainingAsUsedInCrossEntityDerivedProperty(
				ContainingEntity containingAsUsedInCrossEntityDerivedProperty) {
			this.containingAsUsedInCrossEntityDerivedProperty = containingAsUsedInCrossEntityDerivedProperty;
		}

		public Object getContainingAsIndexedEmbeddedWithCast() {
			return containingAsIndexedEmbeddedWithCast;
		}

		public void setContainingAsIndexedEmbeddedWithCast(Object containingAsIndexedEmbeddedWithCast) {
			this.containingAsIndexedEmbeddedWithCast = containingAsIndexedEmbeddedWithCast;
		}

		public ContainedEmbeddable getEmbeddedAssociations() {
			return embeddedAssociations;
		}

		public void setEmbeddedAssociations(ContainedEmbeddable embeddedAssociations) {
			this.embeddedAssociations = embeddedAssociations;
		}

		public List<ContainedEmbeddable> getElementCollectionAssociations() {
			return elementCollectionAssociations;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

		public List<String> getIndexedElementCollectionField() {
			return indexedElementCollectionField;
		}

		public void setIndexedElementCollectionField(List<String> indexedElementCollectionField) {
			this.indexedElementCollectionField = indexedElementCollectionField;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public List<String> getNonIndexedElementCollectionField() {
			return nonIndexedElementCollectionField;
		}

		public void setNonIndexedElementCollectionField(List<String> nonIndexedElementCollectionField) {
			this.nonIndexedElementCollectionField = nonIndexedElementCollectionField;
		}

		public String getFieldUsedInContainedDerivedField1() {
			return fieldUsedInContainedDerivedField1;
		}

		public void setFieldUsedInContainedDerivedField1(String fieldUsedInContainedDerivedField1) {
			this.fieldUsedInContainedDerivedField1 = fieldUsedInContainedDerivedField1;
		}

		public String getFieldUsedInContainedDerivedField2() {
			return fieldUsedInContainedDerivedField2;
		}

		public void setFieldUsedInContainedDerivedField2(String fieldUsedInContainedDerivedField2) {
			this.fieldUsedInContainedDerivedField2 = fieldUsedInContainedDerivedField2;
		}

		public String getFieldUsedInCrossEntityDerivedField1() {
			return fieldUsedInCrossEntityDerivedField1;
		}

		public void setFieldUsedInCrossEntityDerivedField1(String fieldUsedInCrossEntityDerivedField1) {
			this.fieldUsedInCrossEntityDerivedField1 = fieldUsedInCrossEntityDerivedField1;
		}

		public String getFieldUsedInCrossEntityDerivedField2() {
			return fieldUsedInCrossEntityDerivedField2;
		}

		public void setFieldUsedInCrossEntityDerivedField2(String fieldUsedInCrossEntityDerivedField2) {
			this.fieldUsedInCrossEntityDerivedField2 = fieldUsedInCrossEntityDerivedField2;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath(@PropertyValue(propertyName = "fieldUsedInContainedDerivedField1")),
				@ObjectPath(@PropertyValue(propertyName = "fieldUsedInContainedDerivedField2"))
		})
		public Optional<String> getContainedDerivedField() {
			return computeDerived( Stream.of( fieldUsedInContainedDerivedField1, fieldUsedInContainedDerivedField2 ) );
		}

		static ContainedEntityPrimitives<ContainedEntity, ContainedEmbeddable, ContainingEntity> PRIMITIVES = new ContainedEntityPrimitives<ContainedEntity, ContainedEmbeddable, ContainingEntity>() {
			@Override
			public Class<ContainedEntity> entityClass() {
				return ContainedEntity.class;
			}

			@Override
			public ContainedEntity newInstance(int id) {
				ContainedEntity entity = new ContainedEntity();
				entity.setId( id );
				return entity;
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbedded() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbedded,
						ContainedEntity::getContainingAsIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsNonIndexedEmbedded,
						ContainedEntity::getContainingAsNonIndexedEmbedded );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedShallowReindexOnUpdate() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbeddedShallowReindexOnUpdate,
						ContainedEntity::getContainingAsIndexedEmbeddedShallowReindexOnUpdate );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbeddedNoReindexOnUpdate,
						ContainedEntity::getContainingAsIndexedEmbeddedNoReindexOnUpdate );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsUsedInCrossEntityDerivedProperty() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsUsedInCrossEntityDerivedProperty,
						ContainedEntity::getContainingAsUsedInCrossEntityDerivedProperty );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainingEntity> containingAsIndexedEmbeddedWithCast() {
				return PropertyAccessor.create( ContainedEntity::setContainingAsIndexedEmbeddedWithCast );
			}

			@Override
			public PropertyAccessor<ContainedEntity, ContainedEmbeddable> embeddedAssociations() {
				return PropertyAccessor.create( ContainedEntity::setEmbeddedAssociations, ContainedEntity::getEmbeddedAssociations );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, ContainedEmbeddable, List<ContainedEmbeddable>> elementCollectionAssociations() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getElementCollectionAssociations );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> indexedField() {
				return PropertyAccessor.create( ContainedEntity::setIndexedField );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> nonIndexedField() {
				return PropertyAccessor.create( ContainedEntity::setNonIndexedField );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> indexedElementCollectionField() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getIndexedElementCollectionField,
						ContainedEntity::setIndexedElementCollectionField );
			}

			@Override
			public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> nonIndexedElementCollectionField() {
				return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
						ContainedEntity::getNonIndexedElementCollectionField,
						ContainedEntity::setNonIndexedElementCollectionField );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField1() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInContainedDerivedField1 );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField2() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInContainedDerivedField2 );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField1() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInCrossEntityDerivedField1 );
			}

			@Override
			public PropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField2() {
				return PropertyAccessor.create( ContainedEntity::setFieldUsedInCrossEntityDerivedField2 );
			}
		};
	}

	public static class ContainedEmbeddable {

		@OneToOne
		@JoinColumn(name = "CEmbIdxEmbedded")
		// TODO Remove the "emb" prefix from this field when HHH-15604 gets fixed (it's just a workaround)
		private ContainingEntity embContainingAsIndexedEmbedded;

		@OneToOne
		@JoinColumn(name = "CEmbNonIdxEmbedded")
		// TODO Remove the "emb" prefix from this field when HHH-15604 gets fixed (it's just a workaround)
		private ContainingEntity embContainingAsNonIndexedEmbedded;

		public ContainingEntity getEmbContainingAsIndexedEmbedded() {
			return embContainingAsIndexedEmbedded;
		}

		public void setEmbContainingAsIndexedEmbedded(ContainingEntity embContainingAsIndexedEmbedded) {
			this.embContainingAsIndexedEmbedded = embContainingAsIndexedEmbedded;
		}

		public ContainingEntity getEmbContainingAsNonIndexedEmbedded() {
			return embContainingAsNonIndexedEmbedded;
		}

		public void setEmbContainingAsNonIndexedEmbedded(ContainingEntity embContainingAsNonIndexedEmbedded) {
			this.embContainingAsNonIndexedEmbedded = embContainingAsNonIndexedEmbedded;
		}

		static ContainedEmbeddablePrimitives<ContainedEmbeddable, ContainingEntity> PRIMITIVES = new ContainedEmbeddablePrimitives<ContainedEmbeddable, ContainingEntity>() {
			@Override
			public ContainedEmbeddable newInstance() {
				return new ContainedEmbeddable();
			}

			@Override
			public PropertyAccessor<ContainedEmbeddable, ContainingEntity> containingAsIndexedEmbedded() {
				return PropertyAccessor.create( ContainedEmbeddable::setEmbContainingAsIndexedEmbedded,
						ContainedEmbeddable::getEmbContainingAsIndexedEmbedded
				);
			}

			@Override
			public PropertyAccessor<ContainedEmbeddable, ContainingEntity> containingAsNonIndexedEmbedded() {
				return PropertyAccessor.create( ContainedEmbeddable::setEmbContainingAsNonIndexedEmbedded,
						ContainedEmbeddable::getEmbContainingAsNonIndexedEmbedded
				);
			}
		};
	}

}
