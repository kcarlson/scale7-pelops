package org.scale7.cassandra.pelops;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.scale7.portability.SystemProxy;
import org.slf4j.Logger;

public class ManagerOperand {
	private static final Logger logger = SystemProxy.getLoggerFromFactory(ManagerOperand.class);

	static final int SAFE_NODE_CHANGE_DELAY = 60000;
	static final int RETRY_NODE_DELAY = 1000;
	static final int CHANGE_NODE_DELAY = 500;
	static final int MAX_ATTEMPTS = 8;

	private Cluster cluster;
	private String keyspace;
	private Node[] nodesSnapshot;
    private int chosenNodeIdx = 0;
	private long lastNodeWrite = 0;
	private int safeNodeChangeDelay;
    private IConnection connection;

	ManagerOperand(Cluster cluster, String keyspace, int safeNodeChangeDelay) {
    	this.cluster = cluster;
    	this.keyspace = keyspace;
    	this.safeNodeChangeDelay = safeNodeChangeDelay;
    	nodesSnapshot = cluster.getNodes();
	}

	ManagerOperand(Cluster cluster, String keyspace) {
		this(cluster, keyspace, SAFE_NODE_CHANGE_DELAY);
	}

    ManagerOperand(Cluster cluster) {
    	this(cluster, null, SAFE_NODE_CHANGE_DELAY);
    }

    /**
     * Acquire connections to cluster nodes in a manner that reduces the likelihood of synchronization issues
     * in the event of failure. See http://wiki.apache.org/cassandra/FAQ#no_keyspaces for an explanation.
     * @return
     * @throws Exception
     */
	protected void openClient() throws Exception {
		int attempts = 0;
		while (true) {
			try {
                Node chosenNode = nodesSnapshot[chosenNodeIdx];

                connection = new Connection(chosenNode, keyspace);
                connection.open();

	            lastNodeWrite = System.currentTimeMillis();
	            return;
			} catch (Exception e) {
				closeClient();
				attempts++;
				if (attempts < MAX_ATTEMPTS &&
					(e instanceof TimedOutException ||
	                 e instanceof TTransportException ||
	                 e instanceof UnavailableException)) {
					if ((System.currentTimeMillis() - lastNodeWrite) < safeNodeChangeDelay) {
						logger.warn("Retrying opening connection to same node after previous failure to avoid potential synchronization issues");
						Thread.sleep(RETRY_NODE_DELAY);
					} else {
						chosenNodeIdx++;
						if (chosenNodeIdx == nodesSnapshot.length) {
							chosenNodeIdx = 0;
							nodesSnapshot = cluster.getNodes();
						}
						Thread.sleep(CHANGE_NODE_DELAY);
					}
				}
				else {
					throw e;
				}
			}
		}
	}

	private void closeClient() {
        connection.close();
	}

	protected interface IManagerOperation<ReturnType> {
		ReturnType execute(Cassandra.Client conn) throws Exception;
	}

	protected <ReturnType> ReturnType tryOperation(IManagerOperation<ReturnType> operation) throws Exception {

		openClient();
		try {
			// Execute operation
			ReturnType result = operation.execute(connection.getAPI());
			// Close client
			closeClient();
            // Return result!
			return result;
		} catch (Exception e) {
			closeClient();
			throw e;
		}
	}
}
