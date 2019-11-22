package org.hibernate.jpa.test.packaging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.integrationtest.bytecode.model.Book;
import org.hibernate.orm.integrationtest.bytecode.model.Person;

import org.junit.Test;

public class JavassistProxyGenerationTest {

	@Test
	public void testProxyGeneration() throws IOException {
		Properties properties = readProperties();
		properties.setProperty( AvailableSettings.BYTECODE_PROVIDER, "javassist" );

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.applySettings( properties );
		StandardServiceRegistry registry = registryBuilder.build();

		MetadataSources sources = new MetadataSources( registry );
		sources.addAnnotatedClass( Book.class );
		sources.addAnnotatedClass( Person.class );

		Book book = new Book( 98L, "The Prince and the Dressmaker", "978-1-62672-363-4" );

		SessionFactory sf = sources.getMetadataBuilder().build().buildSessionFactory();
		Session session = sf.openSession();
		{
			Transaction tx = session.beginTransaction();
			session.persist( book );
			tx.commit();
		}

		{
			Transaction tx = session.beginTransaction();
			Book proxy = session.getReference( Book.class, book.getId() );

			tx.commit();
		}
	}

	private static Properties readProperties() throws IOException {
		try (InputStream propertiesStream = JavassistProxyGenerationTest.class.getResourceAsStream( "/hibernate.properties" )) {
			Properties properties = new Properties();
			properties.load( propertiesStream );
			return properties;
		}
	}
}
