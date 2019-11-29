/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleFieldTracker;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.internal.MutableEntityEntry;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_ENTRY_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_ENTRY_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME;
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
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(inlineDirtyChecking = true)
public class AllowedEnhancementsAsProxyWithEmbeddableTest
		extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ENHANCER_ENABLE_DIRTY_TRACKING, "true" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { CardGame.class };
	}

	@Before
	public void prepare() {
		doInHibernate( this::sessionFactory, s -> {
			CardGame testEntity = new CardGame( "2", "test" );
			s.persist( testEntity );
		} );
	}

	@After
	public void tearDown() {
		doInHibernate( this::sessionFactory, s -> {
			s.createQuery( "delete from CardGame" ).executeUpdate();
		} );
	}

	@Test
	public void shouldDeclareFieldsInEntityClass() {
		assertThat( CardGame.class )
				.hasDeclaredFields( ENTITY_ENTRY_FIELD_NAME, PREVIOUS_FIELD_NAME, NEXT_FIELD_NAME, TRACKER_FIELD_NAME );
	}

	@Test
	public void shouldDeclareMethodsInEntityClass() {
		assertThat( CardGame.class )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "id", PERSISTENT_FIELD_WRITER_PREFIX + "id" )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "name", PERSISTENT_FIELD_WRITER_PREFIX + "name" )
				.hasDeclaredMethods( ENTITY_INSTANCE_GETTER_NAME, ENTITY_ENTRY_GETTER_NAME )
				.hasDeclaredMethods( PREVIOUS_GETTER_NAME, PREVIOUS_SETTER_NAME, NEXT_GETTER_NAME, NEXT_SETTER_NAME )
				.hasDeclaredMethods( TRACKER_HAS_CHANGED_NAME, TRACKER_CLEAR_NAME, TRACKER_SUSPEND_NAME, TRACKER_GET_NAME );
	}

	@Test
	public void shouldDeclareFieldsInEmbeddedClass() {
		assertThat( Component.class )
				.hasDeclaredFields( TRACKER_COMPOSITE_FIELD_NAME );
	}

	@Test
	public void shouldDeclareMethodsInEmbeddedClass() {
		assertThat( Component.class )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "component", PERSISTENT_FIELD_WRITER_PREFIX + "component" )
				.hasDeclaredMethods( TRACKER_COMPOSITE_SET_OWNER, TRACKER_COMPOSITE_CLEAR_OWNER );
	}

	@Test
	public void testGet() throws Exception {
		doInHibernate( this::sessionFactory, s -> {
			CardGame testEntity = s.get( CardGame.class, "2" );
			assertThat( testEntity )
					.isNotNull();
			assertThat( testEntity )
					.extracting( NEXT_FIELD_NAME ).isNull();
			assertThat( testEntity )
					.extracting( PREVIOUS_FIELD_NAME ).isNull();
			assertThat( testEntity )
					.extracting( ENTITY_ENTRY_FIELD_NAME ).isInstanceOf( MutableEntityEntry.class );
			assertThat( testEntity.getFirstPlayerToken() )
					.extracting( TRACKER_COMPOSITE_FIELD_NAME ).isInstanceOf( CompositeOwnerTracker.class );
		} );
	}

	@Test
	public void shouldCreateTheTracker() throws Exception {
		CardGame entity = new CardGame( "MTG", "Magic the Gathering" );
		assertThat( entity )
				.extracting( NEXT_FIELD_NAME ).isNull();
		assertThat( entity )
				.extracting( PREVIOUS_FIELD_NAME ).isNull();
		assertThat( entity )
				.extracting( ENTITY_ENTRY_FIELD_NAME ).isNull();
		assertThat( entity )
				.extracting( TRACKER_FIELD_NAME ).isInstanceOf( SimpleFieldTracker.class );
		assertThat( entity.getFirstPlayerToken() )
				.extracting( TRACKER_COMPOSITE_FIELD_NAME ).isInstanceOf( CompositeOwnerTracker.class );

		Method hasChangesMethod = CardGame.class.getMethod( TRACKER_HAS_CHANGED_NAME );
		Method getDirtyAttributesMethod = CardGame.class.getMethod( TRACKER_GET_NAME );
		Field compositeOwnersField = Component.class.getDeclaredField( TRACKER_COMPOSITE_FIELD_NAME );
		assertThat( invoke( hasChangesMethod, entity, Boolean.class ) )
				.isTrue();
		assertThat( invoke( getDirtyAttributesMethod, entity, String[].class ) )
				.containsExactly( "name", "firstPlayerToken" );
	}

	@Test
	public void shouldResetTheTracker() throws Exception {
		CardGame entity = new CardGame( "7WD", "7 WOnders duel" );
		Method hasChangesMethod = CardGame.class.getMethod( TRACKER_HAS_CHANGED_NAME );
		Method getDirtyAttributesMethod = CardGame.class.getMethod( TRACKER_GET_NAME );
		Method trackerClearMethod = CardGame.class.getMethod( TRACKER_CLEAR_NAME );

		trackerClearMethod.invoke( entity );

		assertThat( invoke( hasChangesMethod, entity, Boolean.class ) ).isFalse();
		assertThat( invoke( getDirtyAttributesMethod, entity, String[].class ) ).isEmpty();
	}

	@Test
	public void shouldUpdateTheTracker() throws Exception {
		CardGame entity = new CardGame( "SPL", "Splendor" );
		Method hasChangesMethod = CardGame.class.getMethod( TRACKER_HAS_CHANGED_NAME );
		Method getDirtyAttributesMethod = CardGame.class.getMethod( TRACKER_GET_NAME );
		Method trackerClearMethod = CardGame.class.getMethod( TRACKER_CLEAR_NAME );

		trackerClearMethod.invoke( entity );

		entity.setName( "Splendor: Cities of Splendor" );

		assertThat( invoke( hasChangesMethod, entity, Boolean.class ) ).isTrue();
		assertThat( invoke( getDirtyAttributesMethod, entity, String[].class ) )
				.containsExactly( "name", "firstPlayerToken" );
	}

	private static <T> T invoke(Method method, Object entity, Class<T> returnType)
			throws InvocationTargetException, IllegalAccessException {
		return (T) method.invoke( entity );
	}

	private static <T> T invoke(Field field, Object entity, Class<T> returnType)
			throws InvocationTargetException, IllegalAccessException {
		// In case it is private
		field.setAccessible( true );
		return (T) field.get( entity );
	}

	@Embeddable
	public static class Component {
		@Column(name = "first_player_token")
		private String component;

		public Component() {
		}

		private Component(String component) {
			this.component = component;
		}

		public String getComponent() {
			return component;
		}

		public void setComponent(String component) {
			this.component = component;
		}
	}

	@Entity(name = "CardGame")
	public static class CardGame {
		@Id
		private String id;
		private String name;

		@Embedded
		private Component firstPlayerToken;

		public CardGame() {
		}

		private CardGame(String id, String name) {
			this.id = id;
			this.name = name;
			this.firstPlayerToken = createEmbeddedValue( name );
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			this.firstPlayerToken = createEmbeddedValue( name );
		}

		public Component getFirstPlayerToken() {
			return firstPlayerToken;
		}

		public void setFirstPlayerToken(Component firstPlayerToken) {
			this.firstPlayerToken = firstPlayerToken;
		}

		private Component createEmbeddedValue(String name) {
			return new Component( name + " first player token" );
		}
	}

}
