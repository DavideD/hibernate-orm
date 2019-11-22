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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

//TODO: This requires some additional testing:
// = Check the number of elements in the collection when adding the value in both directions
// - Check what happens when the value is added to the collection only
// - I don't need the db to test for enhancements
@RunWith( Arquillian.class )
@RequiresDialect(H2Dialect.class)
public class AssociationManagementEnhacementsOnWildFlyTest {
	private static final String ARCHIVE_NAME = "bytecode-enhancements.war";
	private static final String UNIT_NAME = "primaryPU";

	@Inject
	private BytecodeService service;

	@Deployment
	public static WebArchive deploy() throws Exception {
		Asset persistenceXml = persistenceXml( readProperties(), Book.class, Person.class );

		WebArchive war = ShrinkWrap.create( WebArchive.class, ARCHIVE_NAME )
				.addClasses( Book.class, Person.class, AssociationManagementEnhacementsOnWildFlyTest.class, BytecodeService.class )
				.addAsResource( persistenceXml, "META-INF/persistence.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml");;

		return war;
	}

	private static Properties readProperties() throws IOException {
		try (InputStream propertiesStream = AssociationManagementEnhacementsOnWildFlyTest.class.getResourceAsStream(
				"/hibernate.properties" )) {
			Properties properties = new Properties();
			properties.load( propertiesStream );
			return properties;
		}
	}

	/*
	 * I've added this test to check that if I don't rely on enhancements everything still work.
	 */
	@Test
	public void testDeploymentIsWorking() throws Exception {
		Book saga = new Book( 1L, "Saga – Compendium One", "978-1534313460" );
		Person brianVaughan = new Person( 1L, "Brian K. Vaughan" );
		brianVaughan.getBooks().add( saga );
		saga.setAuthor( brianVaughan );

		service.persist( saga, brianVaughan );
		Person author = service.findAuthorForBook( saga );

		assertEquals( brianVaughan, author );
	}

	@Test
	public void testAssociationManagementEnhancement() throws Exception {
		Book lostConnections = new Book( 5L, "Lost Connections: Why You’re Depressed and How to Find Hope", " 978-1408878729" );
		Person johannHari = new Person( 5L, "Johann Hari" );
		lostConnections.setAuthor( johannHari );

		// Normally you should also have:
		// johannHari.getBooks().add( lostConnections );
		// but in this case it will work because bytecode enhancements are enabled

		assertNotNull( lostConnections.getAuthor() );
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
				.clazz( classArray )
				.excludeUnlistedClasses( true );

		PersistenceDescriptor descriptor = persistenceUnit.getOrCreateProperties()
				// Enable bytecode enhancements
				.createProperty().name( AvailableSettings.HBM2DDL_AUTO ).value( "create-drop" ).up()
				.createProperty().name( AvailableSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT ).value( "true" ).up()
//				.createProperty().name( AvailableSettings.ENHANCER_ENABLE_DIRTY_TRACKING ).value( "true" ).up()
//				.createProperty().name( AvailableSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION ).value( "true" ).up()
				.up().up();

		for ( Map.Entry <Object, Object> entry : extra.entrySet() ) {
			persistenceUnit.getOrCreateProperties()
					.createProperty().name( String.valueOf( entry.getKey() ) ).value( String.valueOf( entry.getValue()  ) );

		}
		return new StringAsset( descriptor.exportAsString() );
	}
}
