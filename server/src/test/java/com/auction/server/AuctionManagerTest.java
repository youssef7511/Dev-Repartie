package com.auction.server;

import com.auction.common.dto.AuctionUpdate;
import com.auction.common.dto.BidRequest;
import com.auction.common.dto.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private static class StubBroadcaster extends MulticastBroadcaster {
        private final List<AuctionUpdate> updates = new ArrayList<>();

        @Override
        protected void initialize() {
            // Skip network initialization for tests
        }

        @Override
        public void broadcast(AuctionUpdate update) {
            updates.add(update);
        }

        @Override
        public void close() {
            // No-op
        }

        List<AuctionUpdate> getUpdates() {
            return updates;
        }
    }

    private AuctionManager auctionManager;
    private StubBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new StubBroadcaster();
        auctionManager = new AuctionManager(broadcaster);
    }

    @Test
    void startAuctionRejectsLowerOrEqualBids() {
        assertTrue(auctionManager.startAuction("Laptop", "Ultrabook", 100.0));

        Product current = auctionManager.getCurrentProduct();
        assertNotNull(current);
        assertTrue(current.isActive());
        assertEquals(100.0, current.getCurrentPrice());

        BidRequest lowerBid = new BidRequest("client-1", "Alice", current.getId(), 90.0);
        assertFalse(auctionManager.placeBid(lowerBid));
        assertEquals(100.0, current.getCurrentPrice());
        assertNull(current.getHighestBidderId());

        BidRequest equalBid = new BidRequest("client-1", "Alice", current.getId(), 100.0);
        assertFalse(auctionManager.placeBid(equalBid));
        assertEquals(100.0, current.getCurrentPrice());
        assertNull(current.getHighestBidderId());
    }

    @Test
    void higherBidIsAcceptedAndBroadcast() {
        assertTrue(auctionManager.startAuction("Phone", "Flagship", 200.0));
        Product current = auctionManager.getCurrentProduct();
        String productId = current.getId();

        BidRequest higherBid = new BidRequest("client-2", "Bob", productId, 250.0);
        assertTrue(auctionManager.placeBid(higherBid));

        assertEquals(250.0, current.getCurrentPrice());
        assertEquals("client-2", current.getHighestBidderId());
        assertEquals("Bob", current.getHighestBidderName());

        List<AuctionUpdate> updates = broadcaster.getUpdates();
        assertEquals(2, updates.size(), "Should broadcast start and bid updates");
        AuctionUpdate lastUpdate = updates.get(updates.size() - 1);
        assertEquals(AuctionUpdate.UpdateType.NEW_BID, lastUpdate.getUpdateType());
        assertEquals(250.0, lastUpdate.getCurrentPrice());
        assertEquals("client-2", lastUpdate.getHighestBidder());
    }

    @Test
    void stopAuctionBroadcastsWinnerAndStoresHistory() {
        assertTrue(auctionManager.startAuction("Console", "Next-gen", 300.0));
        Product current = auctionManager.getCurrentProduct();

        BidRequest first = new BidRequest("c1", "Alice", current.getId(), 320.0);
        BidRequest second = new BidRequest("c2", "Bob", current.getId(), 350.0);
        assertTrue(auctionManager.placeBid(first));
        assertTrue(auctionManager.placeBid(second));

        Product sold = auctionManager.stopAuction();

        assertNotNull(sold);
        assertFalse(sold.isActive());
        assertEquals("c2", sold.getHighestBidderId());
        assertEquals(350.0, sold.getCurrentPrice());
        assertTrue(auctionManager.getSalesHistory().contains(sold));
        assertNull(auctionManager.getCurrentProduct(), "Current auction should be cleared after stop");

        List<AuctionUpdate> updates = broadcaster.getUpdates();
        assertEquals(4, updates.size(), "Start + two bids + close");
        AuctionUpdate finalUpdate = updates.get(updates.size() - 1);
        assertEquals(AuctionUpdate.UpdateType.AUCTION_CLOSED, finalUpdate.getUpdateType());
        assertEquals("c2", finalUpdate.getWinnerId());
        assertEquals(350.0, finalUpdate.getWinningPrice());
    }

    @Test
    void cancelAuctionClearsCurrentAndBroadcastsCancellation() {
        assertTrue(auctionManager.startAuction("Camera", "DSLR", 500.0));
        assertNotNull(auctionManager.getCurrentProduct());

        BidRequest bid = new BidRequest("c3", "Chris", auctionManager.getCurrentProduct().getId(), 520.0);
        assertTrue(auctionManager.placeBid(bid));

        assertTrue(auctionManager.cancelAuction());
        assertNull(auctionManager.getCurrentProduct(), "Current auction should be cleared after cancellation");
        assertTrue(auctionManager.getSalesHistory().isEmpty(), "Cancelled auctions should not enter history");

        List<AuctionUpdate> updates = broadcaster.getUpdates();
        assertEquals(3, updates.size(), "Start + bid + cancel");
        AuctionUpdate last = updates.get(updates.size() - 1);
        assertEquals(AuctionUpdate.UpdateType.AUCTION_CANCELLED, last.getUpdateType());
        assertEquals("Camera", last.getProductName());
    }

    @Test
    void multipleIncreasingBidsProduceOneBroadcastPerBid() {
        assertTrue(auctionManager.startAuction("Tablet", "10-inch", 150.0));
        Product current = auctionManager.getCurrentProduct();

        assertTrue(auctionManager.placeBid(new BidRequest("c1", "Ana", current.getId(), 175.0)));
        assertTrue(auctionManager.placeBid(new BidRequest("c2", "Ben", current.getId(), 200.0)));
        assertTrue(auctionManager.placeBid(new BidRequest("c3", "Cam", current.getId(), 230.0)));

        assertEquals(230.0, current.getCurrentPrice());
        assertEquals("c3", current.getHighestBidderId());

        List<AuctionUpdate> updates = broadcaster.getUpdates();
        assertEquals(4, updates.size(), "Start + three bids");
        assertEquals(AuctionUpdate.UpdateType.NEW_BID, updates.get(updates.size() - 1).getUpdateType());
        assertEquals(230.0, updates.get(updates.size() - 1).getCurrentPrice());
    }
}
