package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.Variant
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilterId by remember { mutableStateOf<Int?>(null) }

    // Navigation toggles
    var showCategoryCRUDManager by remember { mutableStateOf(false) }
    var showProductFormSheet by remember { mutableStateOf<Product?>(null) } // null: hidden, empty Product: Add mode, real Product: Edit mode
    var triggerAddProductMode by remember { mutableStateOf(false) }

    // Reorder sort state
    var isReorderMode by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Calculated product list
    val displayedProducts = remember(products, searchQuery, selectedCategoryFilterId) {
        products.filter { prod ->
            val matchesCat = if (selectedCategoryFilterId == null) true else prod.categoryId == selectedCategoryFilterId
            val matchesSearch = if (searchQuery.isEmpty()) true else {
                prod.name.lowercase().contains(searchQuery.lowercase()) || (prod.description?.lowercase()?.contains(searchQuery.lowercase()) ?: false)
            }
            matchesCat && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Manager", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick, modifier = Modifier.testTag("drawer_menu_button")) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // Reorder Sort Toggle
                    IconButton(onClick = {
                        isReorderMode = !isReorderMode
                        if (isReorderMode) {
                            Toast.makeText(context, "Reorder Mode: Tap handles to move items", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = if (isReorderMode) Icons.Default.DoneAll else Icons.Default.Sort,
                            contentDescription = "Reorder",
                            tint = if (isReorderMode) EmeraldSuccess else IndigoPrimary
                        )
                    }

                    // Category List toggle
                    IconButton(onClick = { showCategoryCRUDManager = true }, modifier = Modifier.testTag("categories_crud_button")) {
                        Icon(imageVector = Icons.Default.Category, contentDescription = "Categories Manager", tint = IndigoPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { triggerAddProductMode = true },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search products catalog...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoPrimary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("products_search_input")
            )

            // Horizontal Categories filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilterId == null,
                    onClick = { selectedCategoryFilterId = null },
                    label = { Text("All Products") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary.copy(alpha = 0.12f),
                        selectedLabelColor = IndigoPrimary
                    )
                )

                categories.forEach { cat ->
                    val selected = selectedCategoryFilterId == cat.id
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategoryFilterId = cat.id },
                        label = { Text("${cat.emoji ?: "📦"} ${cat.name}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary.copy(alpha = 0.12f),
                            selectedLabelColor = IndigoPrimary
                        )
                    )
                }
            }

            // Products list with Swipe-to-delete and Reorder drag handles
            if (displayedProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.Inventory, null, modifier = Modifier.size(64.dp), tint = Slate300)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Products Listed", fontWeight = FontWeight.Bold, color = Slate400)
                        Text("Tap the '+' FAB to add a new product.", color = Slate400, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(displayedProducts, key = { _, prod -> prod.id }) { index, prod ->
                        val catName = categories.find { it.id == prod.categoryId }?.name ?: "No Category"
                        val catEmoji = categories.find { it.id == prod.categoryId }?.emoji ?: "📦"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProductFormSheet = prod }
                                .testTag("product_card_${prod.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Product Image
                                AsyncImage(
                                    model = prod.imageUrl,
                                    contentDescription = prod.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Slate100)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate800,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "$catEmoji $catName",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoPrimary
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = "$${prod.price}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = Slate800
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Stock badge
                                        val stock = prod.stockQuantity ?: 0
                                        Text(
                                            text = "Stock: $stock",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (stock < 15) RoseDanger else Slate400,
                                            modifier = Modifier
                                                .background(if (stock < 15) RoseDanger.copy(alpha = 0.1f) else Slate100, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Interactive Reorder drag handles OR Swipe actions
                                if (isReorderMode) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val mutable = displayedProducts.toMutableList()
                                                    val item = mutable.removeAt(index)
                                                    mutable.add(index - 1, item)
                                                    viewModel.reorderProducts(mutable)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                if (index < displayedProducts.size - 1) {
                                                    val mutable = displayedProducts.toMutableList()
                                                    val item = mutable.removeAt(index)
                                                    mutable.add(index + 1, item)
                                                    viewModel.reorderProducts(mutable)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    // Swipe-to-delete Trash Icon
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteProduct(prod.id)
                                            Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("delete_product_${prod.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RoseDanger.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Category CRUD Manager Bottom Sheet ---
        if (showCategoryCRUDManager) {
            ModalBottomSheet(
                onDismissRequest = { showCategoryCRUDManager = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                var newCatName by remember { mutableStateOf("") }
                var newCatEmoji by remember { mutableStateOf("🥦") }
                var showEmojiGrid by remember { mutableStateOf(false) }

                val emojis = listOf("🥦", "🍎", "🔌", "👕", "🏠", "🧸", "📚", "🥩", "🥤", "📱", "👞", "🍕")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                ) {
                    Text("Category Management", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate800)
                    Spacer(modifier = Modifier.height(16.dp))

                    // List existing categories
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Slate100, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat.emoji ?: "📦")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(cat.name, fontWeight = FontWeight.Bold, color = Slate800)
                            }

                            IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                                Icon(Icons.Default.Delete, null, tint = RoseDanger)
                            }
                        }
                        HorizontalDivider(color = Slate200)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Create new Category Form
                    Text("Add New Category", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji picker trigger
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Slate100)
                                .clickable { showEmojiGrid = !showEmojiGrid }
                                .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(newCatEmoji, fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = newCatName,
                            onValueChange = { newCatName = it },
                            placeholder = { Text("Category name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (showEmojiGrid) {
                        Text("Select Emoji", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate400)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            emojis.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Slate100, CircleShape)
                                        .clickable {
                                            newCatEmoji = emoji
                                            showEmojiGrid = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (newCatName.isNotEmpty()) {
                                viewModel.addCategory(newCatName, newCatEmoji)
                                newCatName = ""
                                Toast.makeText(context, "Category Added!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text("Save Category", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Product Form Sheet (Add / Edit Mode) ---
        val activeFormProduct = showProductFormSheet ?: if (triggerAddProductMode) Product(-1, 1, "", "", 0.0, 0.0, 0, "", null) else null
        if (activeFormProduct != null) {
            val isEdit = activeFormProduct.id != -1

            ModalBottomSheet(
                onDismissRequest = {
                    showProductFormSheet = null
                    triggerAddProductMode = false
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                var name by remember { mutableStateOf(activeFormProduct.name) }
                var description by remember { mutableStateOf(activeFormProduct.description ?: "") }
                var priceString by remember { mutableStateOf(activeFormProduct.price.toString()) }
                var costString by remember { mutableStateOf((activeFormProduct.costPrice ?: 0.0).toString()) }
                var stockString by remember { mutableStateOf((activeFormProduct.stockQuantity ?: 0).toString()) }
                var categoryId by remember { mutableStateOf(activeFormProduct.categoryId) }
                var imageUrl by remember { mutableStateOf(activeFormProduct.imageUrl ?: "") }
                var isActive by remember { mutableStateOf(activeFormProduct.isActive) }

                // Category selection dropdown
                var showCatDropdown by remember { mutableStateOf(false) }

                // Predefined mock product images for simple simulated upload selection
                val sampleImages = listOf(
                    "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400",
                    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400",
                    "https://images.unsplash.com/photo-1627123424574-724758594e93?w=400",
                    "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=400"
                )

                // Variants editor state
                var variantName by remember { mutableStateOf("") }
                var variantOptionInput by remember { mutableStateOf("") }
                val variantsList = remember { mutableStateListOf<Variant>().apply { addAll(activeFormProduct.variants) } }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 50.dp)
                ) {
                    Text(
                        text = if (isEdit) "Edit Product Details" else "Add New Product",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Slate800,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Image Upload selector
                    Text("Select Product Thumbnail", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate400)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        sampleImages.forEach { url ->
                            val isSelected = imageUrl == url
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) IndigoPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { imageUrl = url }
                            ) {
                                AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop)
                            }
                        }
                    }

                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Or enter Custom Image URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Core attributes
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Product Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = priceString, onValueChange = { priceString = it }, label = { Text("Price ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(value = costString, onValueChange = { costString = it }, label = { Text("Cost ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = stockString, onValueChange = { stockString = it }, label = { Text("Stock Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))

                        // Category select box
                        Box(modifier = Modifier.weight(1f)) {
                            val selectedCat = categories.find { it.id == categoryId }
                            OutlinedButton(
                                onClick = { showCatDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate800)
                            ) {
                                Text(selectedCat?.name ?: "Select Cat")
                            }
                            DropdownMenu(expanded = showCatDropdown, onDismissRequest = { showCatDropdown = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${cat.emoji ?: ""} ${cat.name}") },
                                        onClick = {
                                            categoryId = cat.id
                                            showCatDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Variants builder section
                    Text("PRODUCT ATTRIBUTE VARIANTS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate400)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Display active variants
                            variantsList.forEach { variant ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(variant.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(variant.options.joinToString(", "), fontSize = 10.sp, color = Slate400)
                                    }
                                    IconButton(onClick = { variantsList.remove(variant) }) {
                                        Icon(Icons.Default.RemoveCircle, null, tint = RoseDanger)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = variantName,
                                    onValueChange = { variantName = it },
                                    label = { Text("e.g. Size") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = variantOptionInput,
                                    onValueChange = { variantOptionInput = it },
                                    label = { Text("e.g. S,M,L") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                IconButton(onClick = {
                                    if (variantName.isNotEmpty() && variantOptionInput.isNotEmpty()) {
                                        val options = variantOptionInput.split(",").map { it.trim() }
                                        variantsList.add(Variant(variantName, options))
                                        variantName = ""
                                        variantOptionInput = ""
                                    }
                                }) {
                                    Icon(Icons.Default.AddCircle, null, tint = IndigoPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active in Catalog", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val price = priceString.toDoubleOrNull() ?: 0.0
                            val cost = costString.toDoubleOrNull() ?: 0.0
                            val stock = stockString.toIntOrNull() ?: 0

                            if (name.isNotEmpty()) {
                                if (isEdit) {
                                    viewModel.updateProduct(
                                        activeFormProduct.id, name, description,
                                        price, cost, stock, categoryId, variantsList.toList(), imageUrl, isActive
                                    )
                                    Toast.makeText(context, "Product updated!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addProduct(
                                        name, description, price, cost,
                                        stock, categoryId, variantsList.toList(), imageUrl
                                    )
                                    Toast.makeText(context, "Product saved!", Toast.LENGTH_SHORT).show()
                                }
                                showProductFormSheet = null
                                triggerAddProductMode = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
