/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import java.util.function.Function;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.internal.AnyMappingSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EntitySqmPathSource;
import org.hibernate.metamodel.model.domain.internal.MappedSuperclassSqmPathSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.cte.SqmCteTable;
import org.hibernate.query.sqm.tree.domain.AbstractSqmSpecificPluralPartPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddableDomainType;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.domain.SqmMappedSuperclassDomainType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.Type;

/**
 * Helper for dealing with Hibernate's "mapping model" while processing an SQM which is defined
 * in terms of the JPA/SQM metamodel
 *
 * @author Steve Ebersole
 */
public class SqmMappingModelHelper {
	private SqmMappingModelHelper() {
	}

	/**
	 * Resolve a JPA EntityType descriptor to it's corresponding EntityPersister
	 * in the Hibernate mapping type system
	 */
	public static EntityPersister resolveEntityPersister(
			EntityDomainType<?> entityType,
			MappingMetamodel mappingMetamodel) {
		// Our EntityTypeImpl#getType impl returns the Hibernate entity-name
		// which is exactly what we want
		return mappingMetamodel.getEntityDescriptor( entityType.getHibernateEntityName() );
	}

	public static <J> SqmPathSource<J> resolveSqmKeyPathSource(
			DomainType<J> valueDomainType,
			Bindable.BindableType jpaBindableType,
			boolean isGeneric) {
		return resolveSqmPathSource(
				CollectionPart.Nature.INDEX.getName(),
				valueDomainType,
				jpaBindableType,
				isGeneric
		);
	}

	public static <J> SqmPathSource<J> resolveSqmPathSource(
			String name,
			DomainType<J> valueDomainType,
			Bindable.BindableType jpaBindableType,
			boolean isGeneric) {
		return resolveSqmPathSource(
				name,
				null,
				valueDomainType,
				valueDomainType.getExpressibleJavaType(),
				jpaBindableType,
				isGeneric
		);
	}

	public static <J> SqmPathSource<J> resolveSqmPathSource(
			String name,
			SqmPathSource<J> pathModel,
			DomainType<J> valueDomainType,
			JavaType<?> relationalJavaType,
			Bindable.BindableType jpaBindableType,
			boolean isGeneric) {
		if ( valueDomainType instanceof BasicDomainType<?> ) {
			return new BasicSqmPathSource<>(
					name,
					pathModel,
					(BasicDomainType<J>) valueDomainType,
					relationalJavaType,
					jpaBindableType,
					isGeneric
			);
		}
		else if ( valueDomainType instanceof AnyMappingDomainType<?> ) {
			return new AnyMappingSqmPathSource<>(
					name,
					pathModel,
					(AnyMappingDomainType<J>) valueDomainType,
					jpaBindableType
			);
		}
		else if ( valueDomainType instanceof SqmEmbeddableDomainType<J> embeddableDomainType ) {
			return new EmbeddedSqmPathSource<>(
					name,
					pathModel,
					embeddableDomainType,
					jpaBindableType,
					isGeneric
			);
		}
		else if ( valueDomainType instanceof SqmEntityDomainType<J> entityDomainType ) {
			return new EntitySqmPathSource<>(
					name,
					pathModel,
					entityDomainType,
					jpaBindableType,
					isGeneric
			);
		}
		else if ( valueDomainType instanceof SqmMappedSuperclassDomainType<J> mappedSuperclassDomainType ) {
			return new MappedSuperclassSqmPathSource<>(
					name,
					pathModel,
					mappedSuperclassDomainType,
					jpaBindableType,
					isGeneric
			);
		}
		else {
			throw new IllegalArgumentException(
					"Unrecognized value type Java-type [" + valueDomainType.getTypeName() + "] for plural attribute value"
			);
		}
	}

	public static MappingModelExpressible<?> resolveMappingModelExpressible(
			SqmTypedNode<?> sqmNode,
			MappingMetamodel domainModel,
			Function<NavigablePath,TableGroup> tableGroupLocator) {
		if ( sqmNode instanceof SqmPath ) {
			return resolveSqmPath( (SqmPath<?>) sqmNode, domainModel, tableGroupLocator );
		}

		final SqmExpressible<?> nodeType = sqmNode.getNodeType();
		if ( nodeType instanceof BasicType ) {
			return (BasicType<?>) nodeType;
		}

		return null;
	}

	private static ModelPart resolveSqmPath(
			SqmPath<?> sqmPath,
			MappingMetamodel domainModel,
			Function<NavigablePath,TableGroup> tableGroupLocator) {

		if ( sqmPath instanceof SqmTreatedPath<?, ?> treatedPath ) {
			final ManagedDomainType<?> treatTarget = treatedPath.getTreatTarget();
			if ( treatTarget.getPersistenceType() == Type.PersistenceType.ENTITY ) {
				final EntityDomainType<?> treatTargetType = (EntityDomainType<?>) treatTarget;
				return domainModel.findEntityDescriptor( treatTargetType.getHibernateEntityName() );
			}
		}

		// see if the LHS is treated
		if ( sqmPath.getLhs() instanceof SqmTreatedPath<?, ?> treatedPath ) {
			final ManagedDomainType<?> treatTarget = treatedPath.getTreatTarget();
			if ( treatTarget.getPersistenceType() == Type.PersistenceType.ENTITY ) {
				final EntityPersister container = domainModel.findEntityDescriptor( treatTarget.getTypeName() );
				return container.findSubPart( sqmPath.getNavigablePath().getLocalName(), container );
			}
		}

		// Plural path parts are not joined and thus also have no table group
		if ( sqmPath instanceof AbstractSqmSpecificPluralPartPath<?> ) {
			final TableGroup lhsTableGroup = tableGroupLocator.apply( sqmPath.getLhs().getLhs().getNavigablePath() );
			final ModelPartContainer pluralPart = (ModelPartContainer) lhsTableGroup.getModelPart().findSubPart(
					sqmPath.getLhs().getReferencedPathSource().getPathName(),
					null
			);
			final CollectionPart collectionPart = (CollectionPart) pluralPart.findSubPart(
					sqmPath.getReferencedPathSource()
							.getPathName(),
					null
			);
			// For entity collection parts, we must return the entity mapping type,
			// as that is the mapping type of the expression
			if ( collectionPart instanceof EntityCollectionPart ) {
				return ( (EntityCollectionPart) collectionPart ).getAssociatedEntityMappingType();
			}
			return collectionPart;
		}

		if ( sqmPath.getLhs() == null ) {
			final SqmPathSource<?> referencedPathSource = sqmPath.getReferencedPathSource();
			if ( referencedPathSource instanceof EntityDomainType<?> entityDomainType ) {
				return domainModel.findEntityDescriptor( entityDomainType.getHibernateEntityName() );
			}
			assert referencedPathSource instanceof SqmCteTable<?>;
			return null;
		}
		final TableGroup lhsTableGroup = tableGroupLocator.apply( sqmPath.getLhs().getNavigablePath() );
		final ModelPartContainer modelPart;
		if ( lhsTableGroup == null ) {
			modelPart = (ModelPartContainer) resolveSqmPath( sqmPath.getLhs(), domainModel, tableGroupLocator );
			if ( modelPart == null ) {
				// There are many reasons for why this situation can happen,
				// but they all boil down to a parameter being compared against a SqmPath.

				// * If the parameter is used in multiple queries (CTE or subquery),
				// resolving the parameter type based on a SqmPath from a query context other than the current one will fail.

				// * If the parameter is compared to paths with a polymorphic root,
				// the parameter has a SqmPath set as SqmExpressible
				// which is still referring to the polymorphic navigable path,
				// but during query splitting, the SqmRoot in the query is replaced with a root for a subtype.
				// Unfortunately, we can't copy the parameter to reset the SqmExpressible,
				// because we currently build only a single DomainParameterXref, instead of one per query split,
				// so we have to handle this here instead
				return null;
			}
		}
		else {
			modelPart = lhsTableGroup.getModelPart();
		}
		return modelPart.findSubPart( sqmPath.getReferencedPathSource().getPathName(), null );
	}

	public static EntityMappingType resolveExplicitTreatTarget(
			SqmPath<?> sqmPath,
			SqmToSqlAstConverter converter) {
		final SqmPath<?> parentPath = sqmPath.getLhs();
		if ( parentPath instanceof SqmTreatedPath<?, ?> treatedPath ) {
			final ManagedDomainType<?> treatTarget = treatedPath.getTreatTarget();
			if ( treatTarget.getPersistenceType() == Type.PersistenceType.ENTITY ) {
				return resolveEntityPersister(
						( (EntityDomainType<?>) treatTarget ),
						converter.getCreationContext().getMappingMetamodel()
				);
			}
		}

		return null;
	}
}
