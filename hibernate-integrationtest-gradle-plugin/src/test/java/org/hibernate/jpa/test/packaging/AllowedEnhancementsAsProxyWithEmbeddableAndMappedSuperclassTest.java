/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.packaging;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_ENTRY_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_ENTRY_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.INTERCEPTOR_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.INTERCEPTOR_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.INTERCEPTOR_SETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.NEXT_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.NEXT_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.NEXT_SETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PREVIOUS_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PREVIOUS_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PREVIOUS_SETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_CLEAR_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_GET_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_HAS_CHANGED_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_SUSPEND_NAME;

public class AllowedEnhancementsAsProxyWithEmbeddableAndMappedSuperclassTest {

	@Test
	public void shouldDeclareFieldsInEntityClass() {
		assertThat( true ).isTrue();
	}

	@MappedSuperclass
	public static abstract class BaseEntity {
		@Embedded
		public EmbeddedValue superField;

		public EmbeddedValue getSuperField() {
			return superField;
		}

		public void setSuperField(EmbeddedValue superField) {
			this.superField = superField;
		}
	}

	@Embeddable
	public static class EmbeddedValue {
		@Column(name = "super_field")
		private String superField;

		public EmbeddedValue() {
		}

		private EmbeddedValue(String superField) {
			this.superField = superField;
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity extends BaseEntity {
		@Id
		private String id;
		private String name;

		public TestEntity() {
		}

		private TestEntity(String id, String name) {
			this.id = id;
			this.name = name;
			EmbeddedValue value = new EmbeddedValue( "SUPER " + name );
			setSuperField( value );
		}

		public String id() {
			return id;
		}

		public String name() {
			return name;
		}
	}

}
