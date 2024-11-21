
# DDoS Detection Using Entropy

## **Overview**
This project implements a **Distributed Denial of Service (DDoS)** detection system using entropy calculations. It employs a **client-server model** to simulate data packet transmission and monitors the entropy of incoming traffic to detect potential DDoS attacks.

---

## **Features**
- Client-server architecture for simulating real-world data transmission.
- Entropy-based algorithm to detect abnormal traffic patterns.
- Threshold-based alert mechanism for detecting potential DDoS attacks.
- Modular design for easy integration and testing.

---

## **Technologies Used**
- **Programming Language**: Java
- **Libraries**: 
  - `java.io` for file and network I/O
  - `java.net` for socket communication
- **Tools**:
  - IDE: IntelliJ IDEA / Eclipse
  - Build Tool: Maven (optional)
  - Version Control: Git

---

## **Project Structure**
```plaintext
DDoS-Detection-Using-Entropy/
├── src/
│   ├── com/rhgtask/
│   │   ├── Client.java          # Sends data packets to the server
│   │   ├── Server.java          # Receives packets, calculates entropy
│   │   ├── EntropyUtils.java    # Utility to calculate entropy
│   │   ├── DetectionAlgorithm.java # Implements the detection logic
│   └── test-data/
│       ├── packets.txt          # Sample data packets for testing
├── README.md                    # Project documentation
```

---

## **How It Works**
1. The **Client** sends simulated data packets to the **Server**.
2. The **Server** collects these packets and calculates their entropy.
3. If the entropy exceeds a predefined threshold, the system identifies it as a potential DDoS attack.
4. The **EntropyUtils** class implements the entropy calculation logic.

---

## **Setup Instructions**
### **Prerequisites**
- Java Development Kit (JDK) 8 or higher installed.
- IDE (IntelliJ IDEA, Eclipse, or similar).
- Git for version control.

### **Steps**
1. Clone the repository:
   ```bash
   git clone https://github.com/username/DDoS-Detection-Using-Entropy.git
   ```
2. Open the project in your IDE.
3. Run the `Server.java` file to start the server.
4. Run the `Client.java` file to send packets.

---

## **Testing**
1. Modify `packets.txt` in the `test-data` folder to simulate different packet patterns.
2. Adjust the threshold in `Server.java` to test the detection sensitivity.
3. Observe the server logs for entropy values and attack detection messages.

---

## **Screenshots**
### Server Output:
Attached above in the folder

---

## **Future Enhancements**
- Enhance the entropy calculation algorithm for more accuracy.
- Implement a GUI for better visualization.
- Support for multi-threaded clients to simulate real-world traffic.

---




