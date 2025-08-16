import 'package:flutter/material.dart';
import '../models/customer.dart';
import '../models/order.dart';
import '../models/customer_stats.dart';
import '../services/firebase_service.dart';
import '../utils/date_utils.dart' as custom_date_utils;
import '../utils/currency_utils.dart';
import '../widgets/order_list_item.dart';
import '../widgets/stat_item.dart';

class CustomerDetailScreen extends StatefulWidget {
  final Customer customer;

  const CustomerDetailScreen({super.key, required this.customer});

  @override
  _CustomerDetailScreenState createState() => _CustomerDetailScreenState();
}

class _CustomerDetailScreenState extends State<CustomerDetailScreen> {
  final FirebaseService _firebaseService = FirebaseService();
  List<Order> _orders = [];
  bool _isLoading = true;
  CustomerStats? _stats;

  @override
  void initState() {
    super.initState();
    _loadOrders();
  }

  Future<void> _loadOrders() async {
    setState(() => _isLoading = true);
    
    try {
      final orders = await _firebaseService.getOrdersByCustomer(widget.customer.id);
      final stats = _calculateStats(orders);
      
      setState(() {
        _orders = orders;
        _stats = stats;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error loading orders: $e')),
      );
    }
  }

  CustomerStats _calculateStats(List<Order> orders) {
    final totalAmount = orders.fold<double>(0, (sum, order) => sum + order.orderAmount);
    final lastOrderDate = orders.isNotEmpty 
        ? orders.map((o) => o.orderDate).reduce((a, b) => a > b ? a : b)
        : null;
    
    return CustomerStats(
      orderCount: orders.length,
      totalSpent: totalAmount,
      lastOrderDate: lastOrderDate,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.customer.name),
        backgroundColor: Colors.blue[600],
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadOrders,
          ),
        ],
      ),
      body: Column(
        children: [
          // Customer Info Card
          Card(
            margin: const EdgeInsets.all(16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      CircleAvatar(
                        radius: 30,
                        backgroundColor: Colors.blue[100],
                        child: Text(
                          widget.customer.name.isNotEmpty ? widget.customer.name[0].toUpperCase() : '?',
                          style: TextStyle(
                            fontSize: 24,
                            color: Colors.blue[800],
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              widget.customer.name,
                              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                            ),
                            if (widget.customer.email.isNotEmpty)
                              Text(widget.customer.email, style: TextStyle(color: Colors.grey[600])),
                            if (widget.customer.phone.isNotEmpty)
                              Text(widget.customer.phone, style: TextStyle(color: Colors.grey[600])),
                          ],
                        ),
                      ),
                    ],
                  ),
                  if (widget.customer.address.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(Icons.location_on, size: 16, color: Colors.grey[600]),
                        const SizedBox(width: 4),
                        Expanded(
                          child: Text(
                            widget.customer.address,
                            style: TextStyle(color: Colors.grey[600]),
                          ),
                        ),
                      ],
                    ),
                  ],
                  const SizedBox(height: 12),
                  Text(
                    'Customer since ${custom_date_utils.DateUtils.formatTimestamp(widget.customer.createdAt)}',
                    style: TextStyle(color: Colors.grey[500], fontSize: 12),
                  ),
                ],
              ),
            ),
          ),
          
          // Stats Card
          if (_stats != null)
            Card(
              margin: const EdgeInsets.symmetric(horizontal: 16),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    StatItem(
                      icon: Icons.shopping_cart,
                      label: 'Orders',
                      value: _stats!.orderCount.toString(),
                    ),
                    StatItem(
                      icon: Icons.attach_money,
                      label: 'Total Spent',
                      value: CurrencyUtils.formatCurrency(_stats!.totalSpent),
                    ),
                    if (_stats!.lastOrderDate != null)
                      StatItem(
                        icon: Icons.schedule,
                        label: 'Last Order',
                        value: custom_date_utils.DateUtils.formatTimestamp(_stats!.lastOrderDate!),
                      ),
                  ],
                ),
              ),
            ),
          
          const SizedBox(height: 16),
          
          // Orders Section
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                const Text(
                  'Orders',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const Spacer(),
                if (_orders.isNotEmpty)
                  Text(
                    '${_orders.length} orders',
                    style: TextStyle(color: Colors.grey[600]),
                  ),
              ],
            ),
          ),
          
          const SizedBox(height: 8),
          
          // Orders List
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _orders.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(Icons.receipt_long, size: 64, color: Colors.grey),
                            const SizedBox(height: 16),
                            Text(
                              'No orders found',
                              style: TextStyle(fontSize: 18, color: Colors.grey[600]),
                            ),
                          ],
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: _loadOrders,
                        child: ListView.builder(
                          itemCount: _orders.length,
                          itemBuilder: (context, index) {
                            final order = _orders[index];
                            return OrderListItem(order: order);
                          },
                        ),
                      ),
          ),
        ],
      ),
    );
  }
}