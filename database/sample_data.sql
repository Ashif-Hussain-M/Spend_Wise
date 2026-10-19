INSERT INTO users (user_id, username, password_hash)
VALUES (1, 'default_user', NULL)
ON DUPLICATE KEY UPDATE username = 'default_user';

INSERT INTO categories (category_id, user_id, category_name, category_type) VALUES
(1, 1, 'Salary', 'INCOME'),
(2, 1, 'Freelancing & Consulting', 'INCOME'),
(3, 1, 'Investment Returns', 'INCOME'),
(4, 1, 'Rent & Maintenance', 'EXPENSE'),
(5, 1, 'Groceries & Supplies', 'EXPENSE'),
(6, 1, 'Dining Out & Delivery', 'EXPENSE'),
(7, 1, 'Utilities & Broadband', 'EXPENSE'),
(8, 1, 'Fuel & Transit', 'EXPENSE'),
(9, 1, 'Shopping & Electronics', 'EXPENSE'),
(10, 1, 'Entertainment & Subscriptions', 'EXPENSE')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name);

INSERT INTO budgets (user_id, category_id, budget_month, budget_year, budget_amount) VALUES
(1, NULL, 7, 2026, 65000.00),
(1, 4, 7, 2026, 25000.00),
(1, 5, 7, 2026, 12000.00),
(1, 6, 7, 2026, 6000.00),

(1, NULL, 8, 2026, 65000.00),
(1, 4, 8, 2026, 25000.00),
(1, 5, 8, 2026, 12000.00),
(1, 6, 8, 2026, 6000.00),

(1, NULL, 9, 2026, 65000.00),
(1, 4, 9, 2026, 25000.00),
(1, 5, 9, 2026, 12000.00),
(1, 6, 9, 2026, 6000.00),

(1, NULL, 10, 2026, 70000.00),
(1, 4, 10, 2026, 25000.00),
(1, 5, 10, 2026, 12000.00),
(1, 6, 10, 2026, 6000.00),
(1, 9, 10, 2026, 8000.00)
ON DUPLICATE KEY UPDATE budget_amount = VALUES(budget_amount);

INSERT INTO transactions (user_id, category_id, transaction_type, amount, transaction_date, description) VALUES
(1, 1, 'INCOME', 95000.00, '2026-07-01', 'Monthly Salary Credit - Tech Corp'),
(1, 4, 'EXPENSE', 25000.00, '2026-07-02', 'Apartment Rent Indiranagar'),
(1, 7, 'EXPENSE', 3200.00, '2026-07-04', 'BESCOM Electricity Bill & Airtel Fiber'),
(1, 5, 'EXPENSE', 2850.00, '2026-07-05', 'Nature Basket Weekly Organic Groceries'),
(1, 8, 'EXPENSE', 2200.00, '2026-07-08', 'Shell Petrol Refueling'),
(1, 6, 'EXPENSE', 1450.00, '2026-07-10', 'Dinner at Toit Brewery'),
(1, 5, 'EXPENSE', 3100.00, '2026-07-15', 'BigBasket Staples Order'),
(1, 9, 'EXPENSE', 3500.00, '2026-07-18', 'Allen Solly Work Shirts'),
(1, 10, 'EXPENSE', 1199.00, '2026-07-22', 'Netflix & Spotify Premium Annual'),

(1, 1, 'INCOME', 95000.00, '2026-08-01', 'Monthly Salary Credit - Tech Corp'),
(1, 2, 'INCOME', 22000.00, '2026-08-03', 'Freelance AI Consultation Project'),
(1, 4, 'EXPENSE', 25000.00, '2026-08-02', 'Apartment Rent Indiranagar'),
(1, 7, 'EXPENSE', 3450.00, '2026-08-04', 'BESCOM Electricity & Water Charges'),
(1, 5, 'EXPENSE', 3200.00, '2026-08-06', 'Supermarket Monthly Provisioning'),
(1, 8, 'EXPENSE', 2400.00, '2026-08-09', 'IOCL Petrol Refuel'),
(1, 6, 'EXPENSE', 1850.00, '2026-08-14', 'Weekend Family Dinner - Meghana Foods'),
(1, 5, 'EXPENSE', 2900.00, '2026-08-19', 'Zepto Quick Grocery Deliveries'),
(1, 9, 'EXPENSE', 4200.00, '2026-08-23', 'Home Decors & Bedding'),

(1, 1, 'INCOME', 95000.00, '2026-09-01', 'Monthly Salary Credit - Tech Corp'),
(1, 3, 'INCOME', 12500.00, '2026-09-02', 'Mutual Fund SIP Dividend Distribution'),
(1, 4, 'EXPENSE', 25000.00, '2026-09-02', 'Apartment Rent Indiranagar'),
(1, 7, 'EXPENSE', 3100.00, '2026-09-05', 'Broadband & Electricity Utility Bills'),
(1, 5, 'EXPENSE', 3400.00, '2026-09-07', 'BigBasket Fresh Vegetables & Groceries'),
(1, 6, 'EXPENSE', 1600.00, '2026-09-12', 'Swiggy Weekend Lunch Deliveries'),
(1, 8, 'EXPENSE', 2300.00, '2026-09-16', 'Shell City Transit Refuel'),
(1, 5, 'EXPENSE', 2700.00, '2026-09-20', 'Dry Fruits & Household Essentials'),
(1, 9, 'EXPENSE', 3800.00, '2026-09-25', 'Festive Ethnic Wear Purchase'),

(1, 1, 'INCOME', 95000.00, '2026-10-01', 'Monthly Salary Credit - Tech Corp'),
(1, 4, 'EXPENSE', 25000.00, '2026-10-02', 'Apartment Rent Indiranagar'),
(1, 7, 'EXPENSE', 3300.00, '2026-10-04', 'BESCOM Electricity Bill & Broadband'),
(1, 5, 'EXPENSE', 3600.00, '2026-10-06', 'Supermarket Groceries & Pantry Restock'),
(1, 8, 'EXPENSE', 2500.00, '2026-10-08', 'HPCL Petrol Refuel'),
(1, 6, 'EXPENSE', 2200.00, '2026-10-11', 'Team Celebration Dinner - Truffles'),
(1, 5, 'EXPENSE', 3100.00, '2026-10-14', 'Zepto Dairy & Snacks Restock'),
(1, 9, 'EXPENSE', 32500.00, '2026-10-15', 'Festive Diwali Sale - Smart 4K TV & Soundbar');
