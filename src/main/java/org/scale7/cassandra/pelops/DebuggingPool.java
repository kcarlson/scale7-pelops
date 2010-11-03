package org.scale7.cassandra.pelops;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.scale7.portability.SystemProxy;
import org.slf4j.Logger;

import java.net.SocketException;
import java.util.Arrays;
import java.util.Random;

import static java.lang.String.format;

/**
 * A basic non-pooled pool impl. A new connection is opened each time the {@link #getConnection()} or
 * {@link #getConnectionExcept(String)} is called.
 *
 * This class is useful for diagnostics.
 */
public class DebuggingPool extends ThriftPoolBase {
    private static final Logger logger = SystemProxy.getLoggerFromFactory(DebuggingPool.class);

    private Cluster cluster;
    private String keyspace;
    private OperandPolicy generalPolicy;
    private Random random;

    public DebuggingPool(Cluster cluster, String keyspace, OperandPolicy generalPolicy) {
        this.cluster = cluster;
        this.keyspace = keyspace;
        this.generalPolicy = generalPolicy;

        this.random = new Random();
    }

    @Override
    public IPooledConnection getConnection() throws Exception {
        Node[] nodes = cluster.getNodes();
        int index = nodes.length == 1 ? 0 : random.nextInt(nodes.length);

        logger.debug("Using node '{}'", nodes[index]);

        PooledConnection connection = new PooledConnection(
                nodes[index], keyspace
        );

        connection.open();

        return connection;
    }

    @Override
    public IPooledConnection getConnectionExcept(String notNode) throws Exception {
        return getConnection();
    }

    @Override
    public void shutdown() {
        // Do nothing.. we do not have a handle on number of unreleased connections
    }

    @Override
    public OperandPolicy getOperandPolicy() {
        return generalPolicy;
    }

    @Override
    public String getKeyspace() {
        return keyspace;
    }

    public class PooledConnection extends Connection implements IPooledConnection {
        public PooledConnection(Node node, String keyspace) throws SocketException, TException, InvalidRequestException {
            super(node, keyspace);
        }

        @Override
        public void release() {
            close();
        }

        @Override
        public void corrupted() {
            // do nothing (closing anyway)
        }
    }
}
