import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../models/customer.dart';
import '../models/order.dart' as crm;

class FirebaseService {
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  final FirebaseAuth _auth = FirebaseAuth.instance;

  String? get currentUserId => _auth.currentUser?.uid;

  CollectionReference get _customersCollection {
    if (currentUserId == null) {
      throw Exception('User not authenticated');
    }
    return _firestore
        .collection('users')
        .doc(currentUserId!)
        .collection('customers');
  }

  CollectionReference _ordersCollection(String customerId) {
    if (currentUserId == null) {
      throw Exception('User not authenticated');
    }
    return _firestore
        .collection('users')
        .doc(currentUserId!)
        .collection('customers')
        .doc(customerId)
        .collection('orders');
  }

  Future<List<Customer>> getAllCustomers() async {
    try {
      final querySnapshot = await _customersCollection
          .where('isDeleted', isEqualTo: false)
          .orderBy('createdAt', descending: true)
          .get();

      return querySnapshot.docs
          .map((doc) => Customer.fromMap(doc.data() as Map<String, dynamic>, doc.id))
          .toList();
    } catch (e) {
      // TODO: Replace with logging framework in production
      return [];
    }
  }

  Future<Customer?> getCustomerById(String customerId) async {
    try {
      final doc = await _customersCollection.doc(customerId).get();
      if (doc.exists) {
        final data = doc.data() as Map<String, dynamic>;
        if (data['isDeleted'] == false) {
          return Customer.fromMap(data, doc.id);
        }
      }
      return null;
    } catch (e) {
      // TODO: Replace with logging framework in production
      return null;
    }
  }

  Future<List<crm.Order>> getOrdersByCustomer(String customerId) async {
    try {
      final querySnapshot = await _ordersCollection(customerId)
          .where('isDeleted', isEqualTo: false)
          .orderBy('orderDate', descending: true)
          .get();

      return querySnapshot.docs
          .map((doc) => crm.Order.fromMap(doc.data() as Map<String, dynamic>, doc.id))
          .toList();
    } catch (e) {
      // TODO: Replace with logging framework in production
      return [];
    }
  }

  Stream<List<Customer>> getCustomersStream() {
    if (currentUserId == null) {
      return Stream.value([]);
    }

    return _customersCollection
        .where('isDeleted', isEqualTo: false)
        .orderBy('createdAt', descending: true)
        .snapshots()
        .map((snapshot) => snapshot.docs
            .map((doc) => Customer.fromMap(doc.data() as Map<String, dynamic>, doc.id))
            .toList());
  }

  Stream<List<crm.Order>> getOrdersStream(String customerId) {
    if (currentUserId == null) {
      return Stream.value([]);
    }

    return _ordersCollection(customerId)
        .where('isDeleted', isEqualTo: false)
        .orderBy('orderDate', descending: true)
        .snapshots()
        .map((snapshot) => snapshot.docs
            .map((doc) => crm.Order.fromMap(doc.data() as Map<String, dynamic>, doc.id))
            .toList());
  }
}