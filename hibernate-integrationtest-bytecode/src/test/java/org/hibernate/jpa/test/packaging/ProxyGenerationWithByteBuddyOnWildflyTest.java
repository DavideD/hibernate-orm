/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.packaging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.integrationtest.bytecode.controller.BytecodeService;
import org.hibernate.orm.integrationtest.bytecode.model.Book;
import org.hibernate.orm.integrationtest.bytecode.model.Person;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnitTransactionType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

//TODO: This requires some additional testing:
// = Check the number of elements in the collection when adding the value in both directions
// - Check what happens when the value is added to the collection only
// - I don't need the db to test for enhancements
@RunWith( Arquillian.class )
@RequiresDialect(H2Dialect.class)
public class ProxyGenerationWithByteBuddyOnWildflyTest {
	private static final String ARCHIVE_NAME = "bytecode-enhancements.war";
	private static final String UNIT_NAME = "primaryPU";

	@Inject
	private BytecodeService service;

	@PersistenceContext
	private EntityManager em;

	@Deployment
	public static WebArchive deploy() throws Exception {
		Asset persistenceXml = persistenceXml( readProperties(), Book.class, Person.class );

		WebArchive war = ShrinkWrap.create( WebArchive.class, ARCHIVE_NAME )
				.addClasses( Book.class, Person.class, ProxyGenerationWithByteBuddyOnWildflyTest.class, BytecodeService.class )
				.addAsResource( persistenceXml, "META-INF/persistence.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml");;

		return war;
	}

	@Test
	public void testByteCodekProvider() {
		Book saga = new Book( 1L, "Saga – Compendium One", "978-1534313460" );
		Person brianVaughan = new Person( 1L, "Brian K. Vaughan" );
		brianVaughan.getBooks().add( saga );
		saga.setAuthor( brianVaughan );

		service.persist( saga, brianVaughan );
		Book bookReference = service.bookReference( saga.getId() );

		Book reference = em.getReference( Book.class, saga.getId() );
		assertNotEquals( Book.class, reference.getClass() );
	}

	private static Asset persistenceXml(Properties extra, Class<?>... classes) {
		String[] classArray = Arrays.stream( classes )
				.map( Class::getName )
				.collect( Collectors.toUnmodifiableList() )
				.toArray( new String[classes.length] );

		PersistenceUnit<PersistenceDescriptor> persistenceUnit = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( UNIT_NAME )
				.transactionType( PersistenceUnitTransactionType._JTA )
				.clazz( classArray )
				.excludeUnlistedClasses( true );

		PersistenceDescriptor descriptor = persistenceUnit.getOrCreateProperties()
				.createProperty().name( AvailableSettings.BYTECODE_PROVIDER ).value( "javassist" ).up()
				.up().up();

		for ( Map.Entry <Object, Object> entry : extra.entrySet() ) {
			persistenceUnit.getOrCreateProperties()
					.createProperty().name( String.valueOf( entry.getKey() ) ).value( String.valueOf( entry.getValue()  ) );

		}
		return new StringAsset( descriptor.exportAsString() );
	}

	private static Properties readProperties() throws IOException {
		try (InputStream propertiesStream = ProxyGenerationWithByteBuddyOnWildflyTest.class.getResourceAsStream( "/hibernate.properties" )) {
			Properties properties = new Properties();
			properties.load( propertiesStream );
			return properties;
		}
	}
}
