class Order {
  final String id;
  final String customerId;
  final String productName;
  final double orderAmount;
  final int orderDate;
  final String status;
  final String notes;
  final bool isDeleted;
  final int createdAt;
  final int updatedAt;
  final String syncState;

  Order({
    required this.id,
    required this.customerId,
    required this.productName,
    required this.orderAmount,
    required this.orderDate,
    this.status = 'PENDING',
    this.notes = '',
    this.isDeleted = false,
    required this.createdAt,
    required this.updatedAt,
    this.syncState = 'SYNCED',
  });

  factory Order.fromMap(Map<String, dynamic> map, String documentId) {
    return Order(
      id: documentId,
      customerId: map['customerId'] ?? '',
      productName: map['productName'] ?? '',
      orderAmount: (map['orderAmount'] ?? 0).toDouble(),
      orderDate: map['orderDate'] ?? 0,
      status: map['status'] ?? 'PENDING',
      notes: map['notes'] ?? '',
      isDeleted: map['isDeleted'] ?? false,
      createdAt: map['createdAt'] ?? 0,
      updatedAt: map['updatedAt'] ?? 0,
      syncState: map['syncState'] ?? 'SYNCED',
    );
  }
}