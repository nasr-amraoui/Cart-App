package com.example.cartapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.Socket

class MainActivity : ComponentActivity() {
    private var printerSocket: Socket? = null
    private val printerIp = "192.168.10.200" // Your printer's IP address
    private val printerPort = 9100 // Common port for network printers
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Printer Commands
    private val INIT = byteArrayOf(0x1B, 0x40) // Initialize printer
    private val LINE_FEED = byteArrayOf(0x0A) // New line
    private val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x10) // Full cut with feed
    private val CENTER = byteArrayOf(0x1B, 0x61, 0x01) // Center alignment
    private val LEFT = byteArrayOf(0x1B, 0x61, 0x00) // Left alignment
    private val RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // Right alignment
    private val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // Bold text on
    private val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // Bold text off
    private val DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10) // Double height text
    private val NORMAL_HEIGHT = byteArrayOf(0x1B, 0x21, 0x00) // Normal text

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TicketPrintScreen(
                onPrintClick = { ticketContent ->
                    printTicket(ticketContent)
                }
            )
        }
    }

    private fun connectToPrinter(): Boolean {
        return try {
            if (printerSocket?.isConnected != true) {
                printerSocket = Socket(printerIp, printerPort)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Failed to connect to printer: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            false
        }
    }

    private fun printTicket(ticketContent: String) {
        scope.launch {
            try {
                if (connectToPrinter()) {
                    withContext(Dispatchers.IO) {
                        printerSocket?.let { socket ->
                            val outputStream: OutputStream = socket.getOutputStream()
                            
                            // Initialize printer
                            outputStream.write(INIT)
                            
                            // Header
                            outputStream.write(CENTER)
                            outputStream.write(BOLD_ON)
                            outputStream.write(DOUBLE_HEIGHT)
                            outputStream.write("RECEIPT\n".toByteArray())
                            outputStream.write(NORMAL_HEIGHT)
                            outputStream.write(BOLD_OFF)
                            outputStream.write(LINE_FEED)
                            
                            // Date and Time
                            outputStream.write(CENTER)
                            val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())
                            outputStream.write("$currentTime\n".toByteArray())
                            outputStream.write(LINE_FEED)
                            
                            // Content
                            outputStream.write(LEFT)
                            ticketContent.split("\n").forEach { line ->
                                outputStream.write(line.toByteArray())
                                outputStream.write(LINE_FEED)
                            }
                            
                            // Footer
                            outputStream.write(CENTER)
                            outputStream.write(LINE_FEED)
                            outputStream.write("Thank you for your business!\n".toByteArray())
                            outputStream.write(LINE_FEED)
                            outputStream.write(LINE_FEED)
                            
                            // Cut paper
                            outputStream.write(CUT_PAPER)
                            outputStream.flush()
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Printing...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to print: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        printerSocket?.close()
    }
}

@Composable
fun TicketPrintScreen(onPrintClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Ticket Details", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                val sampleTicket = """
                    Item 1           $10.00
                    Item 2           $15.00
                    Item 3           $20.00
                    ------------------------
                    Total:          $45.00
                """.trimIndent()
                onPrintClick(sampleTicket)
            }
        ) {
            Text("Print Ticket")
        }
    }
}

@Composable
@Preview
fun DefaultPreview() {
    TicketPrintScreen(onPrintClick = {})
}
