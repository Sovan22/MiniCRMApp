class CustomerStats {
  final int orderCount;
  final double totalSpent;
  final int? lastOrderDate;

  CustomerStats({
    required this.orderCount,
    required this.totalSpent,
    this.lastOrderDate,
  });
}