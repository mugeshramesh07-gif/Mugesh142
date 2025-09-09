import java.util.*;

enum BagStatus { CHECKED_IN, SECURITY_CLEARED, LOADED, IN_TRANSIT, ARRIVED, CLAIMED, LOST, DAMAGED }
enum ClaimStatus { OPEN, SETTLED, REJECTED }

class Passenger {
    private String paxId;
    private String name;
    private String flightNo;
    private String contact;
    private List<Claim> claims;
    public Passenger(String paxId, String name, String flightNo, String contact) {
        this.paxId = paxId;
        this.name = name;
        this.flightNo = flightNo;
        this.contact = contact;
        this.claims = new ArrayList<>();
    }
    public String getPaxId() { return paxId; }
    public String getName() { return name; }
    public String getFlightNo() { return flightNo; }
    public String getContact() { return contact; }
    public List<Claim> getClaims() { return claims; }
    public void setContact(String contact) { this.contact = contact; }
    public void addClaim(Claim claim) { claims.add(claim); }
    public void printClaims() {
        System.out.println("Claims for " + name + ":");
        for (Claim c : claims) c.display();
    }
    @Override
    public String toString() {
        return String.format("Passenger[%s] %s Flight:%s Contact:%s", paxId, name, flightNo, contact);
    }
}

abstract class Claim {
    private static int counter = 100;
    private String claimId;
    private Passenger passenger;
    private Baggage baggage;
    private double amount;
    private ClaimStatus status;
    public Claim(Passenger passenger, Baggage baggage, double amount) {
        this.claimId = "C" + (counter++);
        this.passenger = passenger;
        this.baggage = baggage;
        this.amount = amount;
        this.status = ClaimStatus.OPEN;
    }
    public String getClaimId() { return claimId; }
    public Passenger getPassenger() { return passenger; }
    public Baggage getBaggage() { return baggage; }
    public double getAmount() { return amount; }
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public abstract void settle();
    public void display() {
        System.out.println(claimId + " | " + passenger.getName() + " | Bag:" + baggage.getBagTag() +
                " | $" + amount + " | Status:" + status);
    }
}

class LossClaim extends Claim {
    public LossClaim(Passenger passenger, Baggage baggage, double amount) {
        super(passenger, baggage, amount);
    }
    @Override
    public void settle() {
        setStatus(ClaimStatus.SETTLED);
        System.out.println("Loss claim " + getClaimId() + " settled at full amount $" + getAmount());
    }
}

class DamageClaim extends Claim {
    public DamageClaim(Passenger passenger, Baggage baggage, double amount) {
        super(passenger, baggage, amount);
    }
    @Override
    public void settle() {
        setStatus(ClaimStatus.SETTLED);
        double payout = getAmount() * 0.5;
        System.out.println("Damage claim " + getClaimId() + " settled at 50% payout $" + payout);
    }
}

class Checkpoint {
    private String id;
    private String name;
    private Date timestamp;
    public Checkpoint(String id, String name) {
        this.id = id;
        this.name = name;
        this.timestamp = new Date();
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public Date getTimestamp() { return timestamp; }
    @Override
    public String toString() {
        return String.format("[%s] %s at %s", id, name, timestamp);
    }
}

class Baggage {
    private String bagTag;
    private double weight;
    private Passenger owner;
    private List<Checkpoint> routeHistory;
    private BagStatus status;
    public Baggage(String bagTag, double weight, Passenger owner) {
        this.bagTag = bagTag;
        this.weight = weight;
        this.owner = owner;
        this.routeHistory = new ArrayList<>();
        this.status = BagStatus.CHECKED_IN;
    }
    public String getBagTag() { return bagTag; }
    public double getWeight() { return weight; }
    public Passenger getOwner() { return owner; }
    public List<Checkpoint> getRouteHistory() { return routeHistory; }
    public BagStatus getStatus() { return status; }
    public void setWeight(double weight) { this.weight = weight; }
    public void updateMovement(Checkpoint cp) {
        routeHistory.add(cp);
        status = BagStatus.IN_TRANSIT;
        System.out.println("Bag " + bagTag + " moved through " + cp);
    }
    public void updateMovement(String cpId, String cpName) {
        Checkpoint cp = new Checkpoint(cpId, cpName);
        updateMovement(cp);
    }
    public void markStatus(BagStatus newStatus) {
        this.status = newStatus;
        System.out.println("Bag " + bagTag + " status updated to " + newStatus);
    }
    public void printRoute() {
        System.out.println("Route history for bag " + bagTag + ":");
        for (Checkpoint c : routeHistory) {
            System.out.println("  " + c);
        }
    }
    @Override
    public String toString() {
        return String.format("Bag[%s] %.1fkg Owner:%s Status:%s", bagTag, weight, owner.getName(), status);
    }
}

class BaggageService {
    private Map<String, Passenger> passengers = new HashMap<>();
    private Map<String, Baggage> bags = new HashMap<>();
    private List<Claim> claims = new ArrayList<>();
    public Passenger registerPassenger(String paxId, String name, String flightNo, String contact) {
        Passenger p = new Passenger(paxId, name, flightNo, contact);
        passengers.put(paxId, p);
        return p;
    }
    public Baggage registerBag(String bagTag, double weight, Passenger owner) {
        Baggage b = new Baggage(bagTag, weight, owner);
        bags.put(bagTag, b);
        return b;
    }
    public void updateMovement(String bagTag, Checkpoint cp) {
        Baggage b = bags.get(bagTag);
        if (b != null) b.updateMovement(cp);
    }
    public void updateMovement(String bagTag, String cpId, String cpName) {
        Baggage b = bags.get(bagTag);
        if (b != null) b.updateMovement(cpId, cpName);
    }
    public Baggage locateBag(String bagTag) {
        return bags.get(bagTag);
    }
    public Claim raiseClaim(String type, Passenger p, Baggage b, double amount) {
        Claim c;
        if ("loss".equalsIgnoreCase(type)) {
            c = new LossClaim(p, b, amount);
        } else {
            c = new DamageClaim(p, b, amount);
        }
        claims.add(c);
        p.addClaim(c);
        System.out.println("Raised " + type + " claim: " + c.getClaimId() + " for passenger " + p.getName());
        return c;
    }
    public void processClaims() {
        for (Claim c : claims) {
            if (c.getStatus() == ClaimStatus.OPEN) {
                c.settle();
            }
        }
    }
    public void printAllBags() {
        System.out.println("All tracked baggage:");
        for (Baggage b : bags.values()) System.out.println("  " + b);
    }
    public void printAllClaims() {
        System.out.println("All claims summary:");
        for (Claim c : claims) c.display();
    }
}

public class BaggageAppMain {
    public static void main(String[] args) {
        BaggageService service = new BaggageService();
        Passenger p1 = service.registerPassenger("P1", "Alice", "AI101", "alice@example.com");
        Passenger p2 = service.registerPassenger("P2", "Bob", "AI102", "bob@example.com");
        Baggage b1 = service.registerBag("BAG001", 18.5, p1);
        Baggage b2 = service.registerBag("BAG002", 22.0, p2);
        service.updateMovement("BAG001", "C1", "Check-in");
        service.updateMovement("BAG001", new Checkpoint("S1", "Security"));
        service.updateMovement("BAG002", "C1", "Check-in");
        System.out.println("Locating BAG001: " + service.locateBag("BAG001"));
        Claim lossClaim = service.raiseClaim("loss", p1, b1, 500.0);
        Claim damageClaim = service.raiseClaim("damage", p2, b2, 300.0);
        service.processClaims();
        b1.printRoute();
        b2.printRoute();
        service.printAllBags();
        service.printAllClaims();
        p1.printClaims();
        p2.printClaims();
    }
}
