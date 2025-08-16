class Customer {
  final String id;
  final String name;
  final String email;
  final String phone;
  final String address;
  final bool isDeleted;
  final int createdAt;
  final int updatedAt;
  final String syncState;

  Customer({
    required this.id,
    required this.name,
    required this.email,
    required this.phone,
    required this.address,
    this.isDeleted = false,
    required this.createdAt,
    required this.updatedAt,
    this.syncState = 'SYNCED',
  });

  factory Customer.fromMap(Map<String, dynamic> map, String documentId) {
    return Customer(
      id: documentId,
      name: map['name'] ?? '',
      email: map['email'] ?? '',
      phone: map['phone'] ?? '',
      address: map['address'] ?? '',
      isDeleted: map['isDeleted'] ?? false,
      createdAt: map['createdAt'] ?? 0,
      updatedAt: map['updatedAt'] ?? 0,
      syncState: map['syncState'] ?? 'SYNCED',
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'email': email,
      'phone': phone,
      'address': address,
      'isDeleted': isDeleted,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
      'syncState': syncState,
    };
  }
}