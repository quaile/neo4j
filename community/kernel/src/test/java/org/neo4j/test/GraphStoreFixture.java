/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.nioneo.store.DynamicRecord;
import org.neo4j.kernel.impl.nioneo.store.NeoStoreRecord;
import org.neo4j.kernel.impl.nioneo.store.NodeRecord;
import org.neo4j.kernel.impl.nioneo.store.PropertyRecord;
import org.neo4j.kernel.impl.nioneo.store.RelationshipRecord;
import org.neo4j.kernel.impl.nioneo.store.StoreAccess;
import org.neo4j.kernel.impl.nioneo.xa.TransactionWriter;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;
import org.neo4j.kernel.impl.transaction.XidImpl;
import org.neo4j.kernel.impl.transaction.xaframework.InMemoryLogBuffer;

public abstract class GraphStoreFixture implements TestRule
{

    private StoreAccess storeAccess;

    public void apply( Transaction transaction ) throws IOException
    {
        applyTransaction( write( transaction, null ) );
    }

    public StoreAccess storeAccess()
    {
        if (storeAccess == null)
        {
            storeAccess = new StoreAccess( directory );
        }
        return storeAccess;
    }

    public File directory()
    {
        return new File( directory );
    }

    public static abstract class Transaction
    {
        public final long startTimestamp = System.currentTimeMillis();
        public final byte[] globalId = XidImpl.getNewGlobalId();

        protected abstract void transactionData( TransactionDataBuilder tx, IdGenerator next );

        private ReadableByteChannel write( IdGenerator idGenerator, int localId, int masterId, int myId, Long txId )
                throws IOException
        {
            InMemoryLogBuffer buffer = new InMemoryLogBuffer();
            TransactionWriter writer = new TransactionWriter( buffer, localId );
            writer.start( globalId, masterId, myId, startTimestamp );

            transactionData( new TransactionDataBuilder( writer ), idGenerator );

            writer.prepare();
            if ( txId != null )
            {
                writer.commit( false, txId );
                writer.done();
            }
            return buffer;
        }
    }

    public class IdGenerator
    {
        public long schema()
        {
            return schemaId++;
        }

        public long node()
        {
            return nodeId++;
        }

        public long nodeLabels()
        {
            return nodeLabelsId++;
        }

        public long relationship()
        {
            return relId++;
        }

        public long property()
        {
            return propId++;
        }

        public long stringProperty()
        {
            return stringPropId++;
        }

        public long arrayProperty()
        {
            return arrayPropId++;
        }

        public int relationshipType()
        {
            return relTypeId++;
        }

        public int propertyKey()
        {
            return propKeyId++;
        }
    }

    public static final class TransactionDataBuilder
    {
        private final TransactionWriter writer;

        public TransactionDataBuilder( TransactionWriter writer )
        {
            this.writer = writer;
        }

        public void createSchema( Collection<DynamicRecord> records )
        {
            try
            {
                writer.createSchema( records );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void propertyKey( int id, String key )
        {
            try
            {
                writer.propertyKey( id, key, id );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void relationshipType( int id, String relationshipType )
        {
            try
            {
                writer.relationshipType( id, relationshipType, id );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void create( NodeRecord node )
        {
            try
            {
                writer.create( node );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void update( NeoStoreRecord record )
        {
            try
            {
                writer.update( record );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void update( NodeRecord before, NodeRecord node )
        {
            try
            {
                writer.update( before, node );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void delete( NodeRecord node )
        {
            try
            {
                writer.delete( node );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void create( RelationshipRecord relationship )
        {
            try
            {
                writer.create( relationship );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void update( RelationshipRecord relationship )
        {
            try
            {
                writer.update( relationship );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void delete( RelationshipRecord relationship )
        {
            try
            {
                writer.delete( relationship );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void create( PropertyRecord property )
        {
            try
            {
                writer.create( property );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void update( PropertyRecord before, PropertyRecord property )
        {
            try
            {
                writer.update( before, property );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        public void delete( PropertyRecord before, PropertyRecord property )
        {
            try
            {
                writer.delete( before, property );
            }
            catch ( IOException e )
            {
                throw ioError( e );
            }
        }

        private Error ioError( IOException e )
        {
            return new Error( "InMemoryLogBuffer should not throw IOException", e );
        }
    }

    protected abstract void generateInitialData( GraphDatabaseService graphDb );

    protected void start( String storeDir )
    {
        // allow for override
    }

    protected void stop()
    {
        if ( storeAccess != null )
        {
            storeAccess.close();
            storeAccess = null;
        }
    }

    protected int myId()
    {
        return 1;
    }

    protected int masterId()
    {
        return -1;
    }

    protected final ReadableByteChannel write( Transaction transaction, Long txId ) throws IOException
    {
        return transaction.write( new IdGenerator(), localIdGenerator++, masterId(), myId(), txId );
    }

    protected void applyTransaction( ReadableByteChannel transaction ) throws IOException
    {
        GraphDatabaseAPI database = (GraphDatabaseAPI) new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(directory).setConfig( configuration( false ) ).newGraphDatabase();
        try
        {
            database.beginTx();
            database.getDependencyResolver().resolveDependency( XaDataSourceManager.class).getNeoStoreDataSource()
                    .applyPreparedTransaction( transaction );
        }
        finally
        {
            database.shutdown();
        }
    }

    protected Map<String, String> configuration( boolean initialData )
    {
        return new HashMap<String, String>();
    }

    private String directory;
    private int localIdGenerator = 0;
    private long schemaId;
    private long nodeId;
    private long nodeLabelsId;
    private long relId;
    private long propId;
    private long stringPropId;
    private long arrayPropId;
    private int relTypeId;
    private int propKeyId;

    private void generateInitialData()
    {
        GraphDatabaseAPI graphDb = (GraphDatabaseAPI) new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(directory).setConfig( configuration( true ) ).newGraphDatabase();
        try
        {
            generateInitialData( graphDb );
            StoreAccess stores = new StoreAccess( graphDb );
            schemaId = stores.getSchemaStore().getHighId();
            nodeId = stores.getNodeStore().getHighId();
            nodeLabelsId = stores.getNodeDynamicLabelStore().getHighId();
            relId = stores.getRelationshipStore().getHighId();
            propId = stores.getPropertyStore().getHighId();
            stringPropId = stores.getStringStore().getHighId();
            arrayPropId = stores.getArrayStore().getHighId();
            relTypeId = (int) stores.getRelationshipTypeTokenStore().getHighId();
            propKeyId = (int) stores.getPropertyKeyNameStore().getHighId();
        }
        finally
        {
            graphDb.shutdown();
        }
    }

    @Override
    public Statement apply( final Statement base, Description description )
    {
        final TargetDirectory.TestDirectory directory = TargetDirectory.forTest( description.getTestClass() )
                                                                       .cleanTestDirectory();
        return directory.apply( new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                GraphStoreFixture.this.directory = directory.directory().getAbsolutePath();
                try
                {
                    generateInitialData();
                    start( GraphStoreFixture.this.directory );
                    try
                    {
                        base.evaluate();
                    }
                    finally
                    {
                        stop();
                    }
                }
                finally
                {
                    GraphStoreFixture.this.directory = null;
                }
            }
        }, description );
    }
}
